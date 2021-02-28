package nl.tudelft.trustchain.ssi.database

import android.content.DialogInterface
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.fragment_database.*
import kotlinx.coroutines.*
import mu.KotlinLogging
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.schema.SchemaManager
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.ssi.PresentAttestationDialog
import nl.tudelft.trustchain.ssi.R
import nl.tudelft.trustchain.ssi.databinding.FragmentDatabaseBinding
import org.json.JSONObject

private val logger = KotlinLogging.logger {}

class DatabaseFragment : BaseFragment(R.layout.fragment_database) {

    private val adapter = ItemAdapter()
    private val binding by viewBinding(FragmentDatabaseBinding::bind)

    lateinit var bitmap: Bitmap

    private val qrCodeUtils by lazy {
        QRCodeUtils(requireContext())
    }

    private var areFabsVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter.registerRenderer(
            DatabaseItemRenderer(
                {
                    setDatabaseItemAction(it)
                },
                {
                    RemoveAttestationDialog(it).show(parentFragmentManager, this.tag)
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

        if (!this::bitmap.isInitialized) {
            lifecycleScope.launch {
                val data = JSONObject()
                data.put("presentation", "authority")
                val myPeer = IPv8Android.getInstance().myPeer
                val publicKey = myPeer.publicKey.keyToBin().toHex()
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
            binding.publicKeyQRCode.setImageBitmap(bitmap)
        }

        binding.myPublicKey.text = getIpv8().myPeer.mid
        loadDatabaseEntries()

        binding.publicKeyQRCode.setOnClickListener {
            if (it.scaleX > 1) {
                it.animate().scaleX(1f).scaleY(1f)
            } else {
                it.animate().scaleX(1.2f).scaleY(1.2f)
            }
        }

        binding.addAttestationFab.visibility = View.GONE
        binding.addAuthorityFab.visibility = View.GONE
        binding.scanAttestationFab.visibility = View.GONE
        binding.addAttestationActionText.visibility = View.GONE
        binding.addAuthorityActionTxt.visibility = View.GONE
        binding.scanAttestationActionText.visibility = View.GONE

        binding.actionFab.setOnClickListener {
            if (!areFabsVisible) {
                binding.addAttestationFab.show()
                binding.addAuthorityFab.show()
                binding.scanAttestationFab.show()
                binding.addAttestationActionText.visibility = View.VISIBLE
                binding.addAuthorityActionTxt.visibility = View.VISIBLE
                binding.scanAttestationActionText.visibility = View.VISIBLE
                areFabsVisible = true
            } else {
                binding.addAttestationFab.hide()
                binding.addAuthorityFab.hide()
                binding.scanAttestationFab.hide()
                binding.addAttestationActionText.visibility = View.GONE
                binding.addAuthorityActionTxt.visibility = View.GONE
                binding.scanAttestationActionText.visibility = View.GONE
                areFabsVisible = false
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

    private fun loadDatabaseEntries() {
        lifecycleScope.launchWhenStarted {
            while (isActive) {
                val attestationCommunity =
                    IPv8Android.getInstance().getOverlay<AttestationCommunity>()!!
                val entries = attestationCommunity.database.getAllAttestations()
                    .mapIndexed { index, blob -> DatabaseItem(index, blob) }

                adapter.updateItems(entries)
                databaseTitle.text = "Attestations"
                txtAttestationCount.text = "${entries.size} entries"
                val textColorResId = if (entries.isNotEmpty()) R.color.green else R.color.red
                val textColor = ResourcesCompat.getColor(resources, textColorResId, null)
                txtAttestationCount.setTextColor(textColor)
                imgEmpty.isVisible = entries.isEmpty()

                delay(5000)
            }
        }
    }

    private fun setDatabaseItemAction(it: DatabaseItem) {
        val dialog = PresentAttestationDialog(it.attestationBlob.idFormat)
        dialog.show(
            parentFragmentManager,
            tag
        )
        lifecycleScope.launch {
            val metadata = it.attestationBlob.metadata
            val manager = SchemaManager()
            manager.registerDefaultSchemas()
            val attestation =
                manager.deserialize(it.attestationBlob.blob, it.attestationBlob.idFormat)

            val signature = it.attestationBlob.signature!!.toHex()
            val attestorKey = it.attestationBlob.attestorKey!!.keyToBin().toHex()
            val key = IPv8Android.getInstance().myPeer.publicKey.keyToBin().toHex()

            val data = JSONObject()
            data.put("presentation", "attestation")
            data.put("metadata", metadata)
            data.put("attestationHash", attestation.getHash().toHex())
            data.put("signature", signature)
            data.put("signee_key", key)
            data.put("attestor_key", attestorKey)
            logger.debug("SSI: Presenting Attestation as QRCode: Size ${data.toString().length}, Data: $data")
            val bitmap = withContext(Dispatchers.Default) {
                qrCodeUtils.createQR(data.toString())!!
            }
            dialog.setQRCode(bitmap)
        }
    }
}

class RemoveAttestationDialog(val item: DatabaseItem) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            builder.setView(view)
                .setPositiveButton(
                    "Delete Attestation",
                    DialogInterface.OnClickListener { _, _ ->
                        IPv8Android.getInstance()
                            .getOverlay<AttestationCommunity>()!!.database.deleteAttestationByHash(
                            item.attestationBlob.attestationHash
                        )
                        Toast.makeText(
                            requireContext(),
                            "Successfully deleted attestation",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
                .setNegativeButton(
                    R.string.cancel,
                    DialogInterface.OnClickListener { _, _ ->
                        Toast.makeText(
                            requireContext(),
                            "Cancelled deletion",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
                .setTitle("Delete Attestation permanently?")
                .setIcon(R.drawable.ic_round_warning_amber_24)
                .setMessage("Note: this action cannot be undone.")
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
