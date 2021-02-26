package nl.tudelft.trustchain.ssi.verifier

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import mu.KotlinLogging
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.ssi.R
import org.json.JSONObject

private val logger = KotlinLogging.logger {}

class VerificationFragment : BaseFragment(R.layout.fragment_verification) {

    private val qrCodeUtils by lazy {
        QRCodeUtils(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        qrCodeUtils.startQRScanner(this, true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val attestationPresentationString =
            qrCodeUtils.parseActivityResult(requestCode, resultCode, data)
        if (attestationPresentationString != null) {
            try {
                val attestationPresentation = JSONObject(attestationPresentationString)
                logger.debug("SSI: Found the following data: $attestationPresentation")
                when (val format = attestationPresentation.get("presentation")) {
                    "authority" -> {
                        val authorityKey =
                            attestationPresentation.getString("public_key").hexToBytes()
                        AuthorityConfirmationDialog(authorityKey).show(
                            parentFragmentManager,
                            this.tag
                        )
                    }
                    "attestation" -> {
                        val metadata = attestationPresentation.getString("metadata")
                        val attestationHash =
                            attestationPresentation.getString("attestationHash").hexToBytes()
                        val signature = attestationPresentation.getString("signature").hexToBytes()
                        val signeeKey = attestationPresentation.getString("signee_key").hexToBytes()
                        val attestorKey =
                            attestationPresentation.getString("attestor_key").hexToBytes()
                        AttestationConfirmationDialog(
                            signeeKey,
                            attestationHash,
                            metadata,
                            signature,
                            attestorKey
                        )
                            .show(parentFragmentManager, this.tag)
                    }
                    else -> throw RuntimeException("Encountered invalid presentation format $format.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Invalid data found", Toast.LENGTH_LONG).show()
                findNavController().navigate(VerificationFragmentDirections.actionVerificationFragmentToDatabaseFragment())
            }
        } else {
            Toast.makeText(requireContext(), "Scan cancelled", Toast.LENGTH_LONG).show()
            findNavController().navigate(VerificationFragmentDirections.actionVerificationFragmentToDatabaseFragment())
        }
    }
}

class AuthorityConfirmationDialog(private val authorityKey: ByteArray) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            builder.setTitle("New Authority Found")
                .setMessage("Address: ${authorityKey.toHex()}")
                .setPositiveButton(
                    "Add",
                    DialogInterface.OnClickListener { _, _ ->
                        val community =
                            IPv8Android.getInstance().getOverlay<AttestationCommunity>()!!
                        val key = defaultCryptoProvider.keyFromPublicBin(
                            authorityKey
                        )
                        val authorityManager = community.trustedAuthorityManager
                        if (!authorityManager.contains(key.keyToHash().toHex())) {
                            community.trustedAuthorityManager.addTrustedAuthority(
                                key
                            )

                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(
                                    context,
                                    "Successfully added new Authority",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(
                                    context,
                                    "Authority already added",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        findNavController().navigate(VerificationFragmentDirections.actionVerificationFragmentToDatabaseFragment())
                    }
                )
                .setNegativeButton(
                    R.string.cancel,
                    DialogInterface.OnClickListener { _, _ ->
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(
                                context,
                                "Cancelled new Authority",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        findNavController().navigate(VerificationFragmentDirections.actionVerificationFragmentToDatabaseFragment())
                    }
                )

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}

class AttestationConfirmationDialog(
    private val attesteeKey: ByteArray,
    private val attestationHash: ByteArray,
    private val metadata: String,
    private val signature: ByteArray,
    private val authorityKey: ByteArray
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Attestation Found")
                .setMessage("Attestation presented by: ${attesteeKey.toHex()}")
                .setPositiveButton(
                    "Verify",
                    DialogInterface.OnClickListener { _, _ ->
                        val community =
                            IPv8Android.getInstance().getOverlay<AttestationCommunity>()!!
                        val success = community.verifyAttestationLocally(
                            Peer(defaultCryptoProvider.keyFromPublicBin(attesteeKey)),
                            attestationHash,
                            metadata,
                            signature,
                            defaultCryptoProvider.keyFromPublicBin(authorityKey)
                        )
                        if (success) {
                            SuccessDialog().show(parentFragmentManager, this.tag)
                        } else {
                            DangerDialog().show(parentFragmentManager, this.tag)
                        }
                    }
                )
                .setNegativeButton(
                    "dismiss",
                    DialogInterface.OnClickListener { _, _ ->
                        DangerDialog().show(parentFragmentManager, this.tag)
                    }
                )
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}

class SuccessDialog : DialogFragment() {

    lateinit var mDialog: Dialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.success_dialog, container, false)
        view.setOnClickListener {
            mDialog.dismiss()
            findNavController().navigate(VerificationFragmentDirections.actionVerificationFragmentToDatabaseFragment())
        }
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        mDialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        return mDialog
    }
}

class DangerDialog : DialogFragment() {

    lateinit var mDialog: Dialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.danger_dialog, container, false)
        view.setOnClickListener {
            mDialog.dismiss()
            findNavController().navigate(VerificationFragmentDirections.actionVerificationFragmentToDatabaseFragment())
        }
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        mDialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        return mDialog
    }
}
