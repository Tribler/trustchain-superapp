package nl.tudelft.trustchain.valuetransfer.dialogs

import android.os.Bundle
import android.os.Handler
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ui.QRScanController
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import nl.tudelft.trustchain.valuetransfer.util.setNavigationBarColor
import org.json.JSONObject
import java.lang.IllegalStateException

class IdentityAttestationVerifyDialog(
    private val attesteeKey: ByteArray,
    private val attestationHash: ByteArray,
    private val metadata: String,
    private val signature: ByteArray,
    private val authorityKey: ByteArray
) : VTDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_identity_attestation_verify, null)

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.behavior.apply {
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }

            setNavigationBarColor(requireContext(), parentActivity, bottomSheetDialog)

            val verificationSummaryView = view.findViewById<ConstraintLayout>(R.id.clVerificationSummary)
            val attestationFromValue = view.findViewById<EditText>(R.id.etAttestationFromValue)
            val attestationAttributeValue = view.findViewById<EditText>(R.id.etAttestationAttributeValue)
            val attestationTypeValue = view.findViewById<EditText>(R.id.etAttestationTypeValue)
            val verifyButton = view.findViewById<Button>(R.id.btnVerifyAttestation)

            val metadataObject = JSONObject(metadata)

            attestationFromValue.setText(attesteeKey.toHex())
            attestationAttributeValue.setText(metadataObject.getString(QRScanController.KEY_ATTRIBUTE))
            attestationTypeValue.setText(metadataObject.getString(QRScanController.KEY_ID_FORMAT))

            val loadingSpinner = view.findViewById<ProgressBar>(R.id.pbLoadingSpinner)

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            verifyButton.setOnClickListener {
                verificationSummaryView.isVisible = false
                loadingSpinner.isVisible = true

                getAttestationCommunity().verifyAttestationLocally(
                    Peer(defaultCryptoProvider.keyFromPublicBin(attesteeKey)),
                    attestationHash,
                    metadata,
                    signature,
                    defaultCryptoProvider.keyFromPublicBin(authorityKey)
                ).let { result ->
                    @Suppress("DEPRECATION")
                    Handler().postDelayed(
                        {
                            loadingSpinner.isVisible = false

                            IdentityAttestationVerificationResultDialog(result).show(parentFragmentManager, tag)
                            bottomSheetDialog.dismiss()
                        },
                        1000
                    )
                }
            }

            bottomSheetDialog
        } ?: throw IllegalStateException(resources.getString(R.string.text_activity_not_null_requirement))
    }
}

class IdentityAttestationVerificationResultDialog(
    private val isValid: Boolean
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = if (isValid) {
                layoutInflater.inflate(R.layout.dialog_identity_attestation_verify_valid, null)
            } else {
                layoutInflater.inflate(R.layout.dialog_identity_attestation_verify_invalid, null)
            }

            bottomSheetDialog.window!!.navigationBarColor = ContextCompat.getColor(
                requireContext(),
                if (isValid) R.color.colorPrimaryValueTransfer else R.color.colorRed
            )

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            bottomSheetDialog
        } ?: throw IllegalStateException(resources.getString(R.string.text_activity_not_null_requirement))
    }
}
