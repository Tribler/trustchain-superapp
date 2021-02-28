package nl.tudelft.trustchain.ssi

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.jaredrummler.blockingdialog.BlockingDialogFragment
import com.jaredrummler.blockingdialog.BlockingDialogManager
import mu.KotlinLogging
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.WalletAttestation
import nl.tudelft.ipv8.attestation.wallet.AttestationBlob
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity
import nl.tudelft.trustchain.common.BaseActivity
import org.json.JSONObject

private val logger = KotlinLogging.logger {}

class SSIMainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val community = IPv8Android.getInstance().getOverlay<AttestationCommunity>()!!
        community.setAttestationRequestCallback(::attestationRequestCallback)
        community.setAttestationRequestCompleteCallback(::attestationRequestCompleteCallback)
        community.setAttestationChunkCallback(::attestationChunkCallback)

        // Register own key as trusted authority.
        community.trustedAuthorityManager.addTrustedAuthority(IPv8Android.getInstance().myPeer.publicKey)
    }

    private fun attestationChunkCallback(peer: Peer, i: Int) {
        logger.info("Received attestation chunk $i from ${peer.mid}.")
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                applicationContext,
                "Received attestation chunk $i from ${peer.mid}.",
                Toast.LENGTH_LONG
            )
                .show()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun attestationRequestCompleteCallback(
        forPeer: Peer,
        attributeName: String,
        attestation: WalletAttestation,
        attributeHash: ByteArray,
        idFormat: String,
        fromPeer: Peer?,
        metaData: String?,
        Signature: ByteArray?
    ) {
        if (fromPeer == null) {
            logger.info("Signed attestation for attribute $attributeName for peer ${forPeer.mid}.")
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    applicationContext,
                    "Sending attestation for $attributeName to peer ${forPeer.mid}",
                    Toast.LENGTH_LONG
                )
                    .show()
            }
        } else {
            logger.info("Received attestation for attribute $attributeName with metadata: $metaData.")
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    applicationContext,
                    "Received Attestation for $attributeName",
                    Toast.LENGTH_LONG
                )
                    .show()
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun attestationRequestCallback(
        peer: Peer,
        attributeName: String,
        metadata: String
    ): ByteArray {
        logger.info("Attestation: called")
        val parsedMetadata = JSONObject(metadata)
        val idFormat = parsedMetadata.optString("id_format", "id_metadata")
        val input =
            BlockingDialogManager.getInstance().showAndWait<String?>(this, AttestationValueDialog())
                ?: throw RuntimeException("User cancelled dialog.")
        logger.info("Signing attestation with value $input with format $idFormat.")
        return when (idFormat) {
            "id_metadata_range_18plus" -> byteArrayOf(input.toByte())
            else -> input.toByteArray()
        }
    }

    override val navigationGraph = R.navigation.nav_graph_ssi
    override val bottomNavigationMenu = R.menu.bottom_navigation_menu2
}

class FireMissilesDialogFragment(val peer: Peer) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val attestationCommunity =
                IPv8Android.getInstance().getOverlay<AttestationCommunity>()!!
            val view = inflater.inflate(R.layout.request_attestation_dialog, null)
            builder.setView(view)
                .setPositiveButton(
                    R.string.fire,
                    DialogInterface.OnClickListener { _, _ ->
                        val attrInput = view.findViewById<TextInputEditText>(R.id.attribute_input)
                        logger.info("Sending attestation for ${attrInput.text} to ${peer.mid}")
                        attestationCommunity.requestAttestation(
                            peer,
                            attrInput.text.toString(),
                            IPv8Android.getIdentityKeySmall(),
                            hashMapOf("id_format" to "id_metadata_range_18plus"),
                            true
                        )
                        Toast.makeText(
                            requireContext(),
                            "Requested attestation for ${attrInput.text} from ${peer.mid}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
                .setNegativeButton(
                    R.string.cancel,
                    DialogInterface.OnClickListener { _, _ -> }
                )
                .setTitle("Request Attestation")
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}

class AttestationValueDialog() : BlockingDialogFragment<String>() {

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog? {
        @Suppress("DEPRECATION")
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = it.layoutInflater
            val view = inflater.inflate(R.layout.attestation_value_dialog, null)
            builder.setView(view)
                .setPositiveButton(
                    R.string.fire,
                    DialogInterface.OnClickListener { _, _ ->
                        val attrInput = view.findViewById<TextInputEditText>(R.id.value_input)
                        setResult(attrInput.text.toString(), false)
                    }
                )
                .setNegativeButton(
                    R.string.cancel,
                    DialogInterface.OnClickListener { _, _ ->
                        setResult("", true)
                    }
                )
                .setTitle("Attestation Requested")
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}

class PresentAttestationDialog(private val attributeName: String) :
    DialogFragment() {

    private var mView: View? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            mView = inflater.inflate(R.layout.present_attestation_dialog, null)
            builder.setView(mView)
                .setTitle("Attestation for $attributeName")
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    fun setQRCode(bitmap: Bitmap) {
        val progressBar = mView!!.findViewById<ProgressBar>(R.id.progressBar)
        if (progressBar.isVisible) {
            progressBar.visibility = View.GONE
        }
        mView!!.findViewById<ImageView>(R.id.qrCodeView).setImageBitmap(bitmap)
    }
}

class VerifyAttestationDialog(val databaseBlob: AttestationBlob) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val attestationCommunity =
                IPv8Android.getInstance().getOverlay<AttestationCommunity>()!!
            val view = inflater.inflate(R.layout.verify_attestation_dialog, null)
            builder.setView(view)
                .setPositiveButton(
                    R.string.fire,
                    DialogInterface.OnClickListener { _, _ ->
                        val addressInput =
                            view.findViewById<TextInputEditText>(R.id.peer_address_input).text.toString()
                        val attributeName =
                            view.findViewById<TextInputEditText>(R.id.attribute_name_input).text.toString()
                        val input = addressInput.split(":").toTypedArray()
                        val ip = input[0]
                        val port = input[1].toInt()
                        var address: IPv4Address? = null
                        for (peer in attestationCommunity.getPeers()) {
                            if (peer.address.toString() == addressInput) {
                                address = IPv4Address(ip, port)
                            }
                        }
                        if (address == null) {
                            throw RuntimeException("IPv4 Address not found")
                        }
                        logger.info("SII: Sending verify request")
                        attestationCommunity.verifyAttestationValues(
                            address,
                            databaseBlob.attestationHash,
                            arrayListOf(attributeName.toByteArray()),
                            ::verifyComplete,
                            databaseBlob.idFormat
                        )
                    }
                )

                .setNegativeButton(
                    R.string.cancel,
                    DialogInterface.OnClickListener { _, _ -> }
                )
                .setTitle("Request Attestation")
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun verifyComplete(hash: ByteArray, values: List<Double>) {
        println(" *** VerifyComplete: $hash, values:")
        values.forEach { print(" $it") }
        println(" ***")
    }
}
