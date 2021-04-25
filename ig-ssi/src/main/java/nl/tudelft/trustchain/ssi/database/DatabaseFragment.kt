package nl.tudelft.trustchain.ssi.database

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.fragment_database.*
import kotlinx.coroutines.*
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.WalletAttestation
import nl.tudelft.ipv8.attestation.identity.DEFAULT_METADATA
import nl.tudelft.ipv8.util.defaultEncodingUtils
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.ssi.Communication
import nl.tudelft.trustchain.ssi.R
import nl.tudelft.trustchain.ssi.attestationRequestCompleteCallback
import nl.tudelft.trustchain.ssi.databinding.FragmentDatabaseBinding
import nl.tudelft.trustchain.ssi.dialogs.attestation.PresentAttestationDialog
import nl.tudelft.trustchain.ssi.dialogs.attestation.RemoveAttestationDialog
import org.json.JSONObject

class DatabaseFragment : BaseFragment(R.layout.fragment_database) {

    private val adapter = ItemAdapter()
    private val binding by viewBinding(FragmentDatabaseBinding::bind)

    lateinit var bitmap: Bitmap

    private val qrCodeUtils by lazy {
        QRCodeUtils(requireContext())
    }

    private var areFABsVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter.registerRenderer(
            DatabaseItemRenderer(
                {
                    setDatabaseItemAction(it)
                },
                {
                    RemoveAttestationDialog(it, ::loadDatabaseEntries).show(
                        parentFragmentManager,
                        this.tag
                    )
                }
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayout.VERTICAL
            )
        )
        binding.myPublicKey.text = Communication.load().myPeer.mid
        setQRCode()
        setFABs()
        loadDatabaseEntriesOnLoop()
    }

    private fun loadDatabaseEntriesOnLoop() {
        lifecycleScope.launchWhenStarted {
            while (isActive) {
                loadDatabaseEntries()
                delay(1500)
            }
        }
    }

    private fun loadDatabaseEntries() {
        val channel =
            Communication.load()
        val entries = channel.getOfflineVerifiableAttributes()
            .mapIndexed { index, blob -> DatabaseItem(index, blob) }

        adapter.updateItems(entries)
        databaseTitle.text = "Attestations"
        txtAttestationCount.text = "${entries.size} entries"
        val textColorResId = if (entries.isNotEmpty()) R.color.green else R.color.red
        val textColor = ResourcesCompat.getColor(resources, textColorResId, null)
        txtAttestationCount.setTextColor(textColor)
        imgEmpty.isVisible = entries.isEmpty()
    }

    private fun setDatabaseItemAction(it: DatabaseItem) {
        val attributeName = it.attestation.attributeName
        val attributeValue = it.attestation.attributeValue

        val dialog = PresentAttestationDialog(
            attributeName,
            attributeValue
        )
        dialog.show(
            parentFragmentManager,
            tag
        )

        // TODO: Optional signatures.
        // If the attestation contains no signature, show an error.
        // if (it.attestation.signature == null) {
        //     lifecycleScope.launch {
        //         // Give ample time for the dialog to be rendered.
        //         delay(500)
        //         dialog.showError()
        //     }
        // } else {
        lifecycleScope.launch {
            val data = JSONObject()
            val channel = Communication.load()
            val (timestamp, challenge) = channel.generateChallenge()
            // TODO: Definitions.
            data.put("presentation", "attestation")
            data.put("challenge", defaultEncodingUtils.encodeBase64ToString(challenge))
            data.put("timestamp", timestamp)
            data.put("metadata", it.attestation.metadataString)
            data.put("attestationHash", it.attestation.attributeHash.toHex())
            data.put("signature", defaultEncodingUtils.encodeBase64ToString(it.attestation.signature))
            data.put("attestor", it.attestation.attestorKeys[0])
            data.put(
                "subject",
                defaultEncodingUtils.encodeBase64ToString(channel.myPeer.publicKey.keyToBin())
            )

            val output =
                defaultEncodingUtils.encodeBase64ToString(data.toString().toByteArray())
            Log.d(
                "ig-ssi",
                "Presenting Attestation as QRCode: Size ${
                    output.length
                }, Data: $data"
            )
            val bitmap = withContext(Dispatchers.Default) {
                qrCodeUtils.createQR(output)!!
            }
            dialog.setQRCode(bitmap)
        }
        // }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setFABs() {
        binding.addAttestationFab.visibility = View.GONE
        binding.addAuthorityFab.visibility = View.GONE
        binding.scanAttestationFab.visibility = View.GONE
        binding.addAttestationActionText.visibility = View.GONE
        binding.addAuthorityActionTxt.visibility = View.GONE
        binding.scanAttestationActionText.visibility = View.GONE

        binding.actionFab.setOnClickListener {
            if (!areFABsVisible) {
                binding.addAttestationFab.show()
                binding.addAuthorityFab.show()
                binding.scanAttestationFab.show()
                binding.addAttestationActionText.visibility = View.VISIBLE
                binding.addAuthorityActionTxt.visibility = View.VISIBLE
                binding.scanAttestationActionText.visibility = View.VISIBLE
                areFABsVisible = true
            } else {
                binding.addAttestationFab.hide()
                binding.addAuthorityFab.hide()
                binding.scanAttestationFab.hide()
                binding.addAttestationActionText.visibility = View.GONE
                binding.addAuthorityActionTxt.visibility = View.GONE
                binding.scanAttestationActionText.visibility = View.GONE
                areFABsVisible = false
            }
        }

        binding.addAttestationFab.setOnClickListener {
            val bundle = bundleOf("qrCodeHint" to "Scan signee public key", "intent" to 0)
            findNavController().navigate(
                DatabaseFragmentDirections.actionDatabaseFragmentToVerificationFragment().actionId,
                bundle
            )
        }

        binding.addAuthorityFab.setOnClickListener {
            val bundle = bundleOf("qrCodeHint" to "Scan signee public key", "intent" to 1)
            findNavController().navigate(
                DatabaseFragmentDirections.actionDatabaseFragmentToVerificationFragment().actionId,
                bundle
            )
        }

        binding.scanAttestationFab.setOnClickListener {
            val bundle = bundleOf("qrCodeHint" to "Scan attestation", "intent" to 2)
            findNavController().navigate(
                DatabaseFragmentDirections.actionDatabaseFragmentToVerificationFragment().actionId,
                bundle
            )
        }
    }

    private fun setQRCode() {
        if (!this::bitmap.isInitialized) {
            lifecycleScope.launch {
                val data = JSONObject()
                data.put("presentation", "authority")
                val myPeer = IPv8Android.getInstance().myPeer
                val publicKey =
                    defaultEncodingUtils.encodeBase64ToString(myPeer.publicKey.keyToBin())
                data.put("public_key", publicKey)
                IPv8Android.getInstance()

                bitmap = withContext(Dispatchers.Default) {
                    qrCodeUtils.createQR(data.toString(), 300)!!
                }
                try {
                    binding.qrCodePlaceHolder.visibility = View.GONE
                    binding.publicKeyQRCode.setImageBitmap(bitmap)
                } catch (e: IllegalStateException) {
                    // This happens if we already switched screens.
                }
            }
        } else {
            binding.qrCodePlaceHolder.visibility = View.GONE
            binding.publicKeyQRCode.setImageBitmap(bitmap)
        }

        binding.publicKeyQRCode.setOnClickListener {
            if (it.scaleX > 1) {
                it.animate().scaleX(1f).scaleY(1f)
            } else {
                it.animate().scaleX(1.2f).scaleY(1.2f)
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun databaseAttestationRequestCompleteCallback(
        forPeer: Peer,
        attributeName: String,
        attestation: WalletAttestation,
        attributeHash: ByteArray,
        idFormat: String,
        fromPeer: Peer?,
        metaData: String?,
        signature: ByteArray?
    ) {
        attestationRequestCompleteCallback(
            forPeer,
            attributeName,
            attestation,
            attributeHash,
            idFormat,
            fromPeer,
            metaData,
            signature,
            requireContext()
        )
        Handler(Looper.getMainLooper()).post {
            try {
                loadDatabaseEntries()
            } catch (e: NullPointerException) {
                // We're no longer in this screen.
            }
        }
    }
}
