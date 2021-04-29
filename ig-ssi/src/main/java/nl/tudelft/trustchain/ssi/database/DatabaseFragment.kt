package nl.tudelft.trustchain.ssi.database

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
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
import nl.tudelft.ipv8.attestation.WalletAttestation
import nl.tudelft.ipv8.attestation.communication.AttestationPresentation
import nl.tudelft.ipv8.attestation.communication.DEFAULT_TIME_OUT
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.ssi.Communication
import nl.tudelft.trustchain.ssi.R
import nl.tudelft.trustchain.ssi.attestationRequestCompleteCallback
import nl.tudelft.trustchain.ssi.databinding.FragmentDatabaseBinding
import nl.tudelft.trustchain.ssi.dialogs.attestation.ID_PICTURE
import nl.tudelft.trustchain.ssi.dialogs.attestation.PresentAttestationDialog
import nl.tudelft.trustchain.ssi.dialogs.attestation.RemoveAttestationDialog
import nl.tudelft.trustchain.ssi.encodeB64
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

const val QR_CODE_VALUE_LIMIT = 2

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

        binding.publicKeyQRCode.setOnClickListener {
            if (it.scaleX > 1) {
                it.animate().scaleX(1f).scaleY(1f)
            } else {
                it.animate().scaleX(1.2f).scaleY(1.2f)
            }
        }

        setQRCode()
        setFABs()
        loadDatabaseEntriesOnLoop()
    }

    private fun loadDatabaseEntriesOnLoop() {
        lifecycleScope.launchWhenStarted {
            while (isActive) {
                loadDatabaseEntries()
                delay(100)
            }
        }
    }

    private fun loadDatabaseEntries() {
        val channel =
            Communication.load()
        val entries = channel.getOfflineVerifiableAttributes()
            .mapIndexed { index, blob -> DatabaseItem(index, blob) }

        val areEqual = entries == adapter.items
        if (!areEqual) {
            adapter.updateItems(entries)
        }

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
            String(attributeValue)
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
            while (isActive) {
                val channel = Communication.load()
                val challenge = channel.generateChallenge()

                var secondaryQRCode: Bitmap? = null
                val presentation: String
                if (it.attestation.attributeValue.size > QR_CODE_VALUE_LIMIT) {
                    presentation =
                        formatAttestationToJSON(it.attestation, channel.myPeer.publicKey, challenge)
                    val secondaryPresentation =
                        formatValueToJSON(
                            it.attestation.attributeValue,
                            challenge,
                            encode = attributeName != ID_PICTURE.toUpperCase(
                                Locale.getDefault()
                            )
                        )
                    secondaryQRCode =
                        QRGEncoder(
                            secondaryPresentation,
                            null,
                            QRGContents.Type.TEXT,
                            1000
                        ).bitmap
                } else {
                    presentation = formatAttestationToJSON(
                        it.attestation,
                        channel.myPeer.publicKey,
                        challenge,
                        it.attestation.attributeValue
                    )
                }

                Log.d(
                    "ig-ssi",
                    "Presenting Attestation as QRCode: Size ${
                        presentation.length
                    }, Data: $presentation"
                )
                // val bitmap = withContext(Dispatchers.Default) {
                //     qrCodeUtils.createQR(presentation, 1000)!!
                // }
                val bitmap = withContext(Dispatchers.Default) {
                    QRGEncoder(presentation, null, QRGContents.Type.TEXT, 1000).bitmap
                }

                dialog.setQRCodes(bitmap, secondaryQRCode)
                dialog.startTimeout(DEFAULT_TIME_OUT)
                delay(DEFAULT_TIME_OUT)
            }
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
                val myPeer = Communication.load().myPeer
                val publicKey =
                    encodeB64(myPeer.publicKey.keyToBin())
                data.put("public_key", publicKey)

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
    }

    private fun formatValueToJSON(
        value: ByteArray,
        challengePair: Pair<Long, ByteArray>,
        encode: Boolean = true
    ): String {
        val data = JSONObject()
        data.put("presentation", "attestation")

        // Challenge
        val challenge = JSONObject()
        val (challengeValue, challengeSignature) = challengePair
        challenge.put("timestamp", challengeValue)
        challenge.put("signature", encodeB64(challengeSignature))
        data.put("challenge", challenge)

        // Value
        val valueString = if (encode) encodeB64(value) else String(value)
        data.put("value", valueString)

        return data.toString()
    }

    private fun formatAttestationToJSON(
        attestation: AttestationPresentation,
        subjectKey: nl.tudelft.ipv8.keyvault.PublicKey,
        challengePair: Pair<Long, ByteArray>,
        value: ByteArray? = null
    ): String {
        val data = JSONObject()
        data.put("presentation", "attestation")

        // TODO: Definitions.
        // AttestationHash
        data.put("attestationHash", encodeB64(attestation.attributeHash))

        // Metadata
        val metadata = JSONObject()
        val (pointer, signature, serializedMD) = attestation.metadata.toDatabaseTuple()
        metadata.put("pointer", encodeB64(pointer))
        metadata.put("signature", encodeB64(signature))
        metadata.put(
            "metadata", String(serializedMD)
        )
        data.put("metadata", metadata)

        // Subject
        data.put(
            "subject",
            encodeB64(subjectKey.keyToBin())
        )

        // Challenge
        val challenge = JSONObject()
        val (challengeValue, challengeSignature) = challengePair
        challenge.put("timestamp", challengeValue)
        challenge.put("signature", encodeB64(challengeSignature))
        data.put("challenge", challenge)

        // Attestors
        val attestors = JSONArray()
        for (attestor in attestation.attestors) {
            val attestorJSON = JSONObject()
            attestorJSON.put(
                "keyHash",
                encodeB64(attestor.first)
            )
            attestorJSON.put(
                "signature",
                encodeB64(attestor.second)
            )
            attestors.put(attestorJSON)
        }
        data.put("attestors", attestors)

        // Value
        if (value != null) {
            data.put("value", encodeB64(value))
        }

        return data.toString()
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
