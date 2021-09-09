package nl.tudelft.trustchain.valuetransfer.dialogs

import android.os.Bundle
import android.os.Handler
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import org.json.JSONObject
import java.lang.IllegalStateException

class IdentityAttestationVerifyDialog(
    private val attesteeKey: ByteArray,
    private val attestationHash: ByteArray,
    private val metadata: String,
    private val signature: ByteArray,
    private val authorityKey: ByteArray
) : DialogFragment() {

    private lateinit var parentActivity: ValueTransferMainActivity
    private lateinit var attestationCommunity: AttestationCommunity

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_identity_attestation_verify, null)

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.behavior.skipCollapsed = true
            bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

            parentActivity = requireActivity() as ValueTransferMainActivity
            attestationCommunity = parentActivity.getCommunity(ValueTransferMainActivity.attestationCommunityTag) as AttestationCommunity

            val verificationSummaryView = view.findViewById<ConstraintLayout>(R.id.clVerificationSummary)
            val attestationFromValue = view.findViewById<EditText>(R.id.etAttestationFromValue)
            val attestationAttributeValue = view.findViewById<EditText>(R.id.etAttestationAttributeValue)
            val attestationTypeValue = view.findViewById<EditText>(R.id.etAttestationTypeValue)
            val verifyButton = view.findViewById<Button>(R.id.btnVerifyAttestation)

            val metadataObject = JSONObject(metadata)

            attestationFromValue.setText(attesteeKey.toHex())
            attestationAttributeValue.setText(metadataObject.getString("attribute"))
            attestationTypeValue.setText(metadataObject.getString("id_format"))

            val loadingSpinner = view.findViewById<ProgressBar>(R.id.pbLoadingSpinner)

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            verifyButton.setOnClickListener {
                verificationSummaryView.isVisible = false
                loadingSpinner.isVisible = true

                attestationCommunity.verifyAttestationLocally(
                    Peer(defaultCryptoProvider.keyFromPublicBin(attesteeKey)),
                    attestationHash,
                    metadata,
                    signature,
                    defaultCryptoProvider.keyFromPublicBin(authorityKey)
                ).let { result ->
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
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}

class IdentityAttestationVerificationResultDialog(
    private val isValid: Boolean
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val view = layoutInflater.inflate(R.layout.dialog_identity_attestation_verify, null)
            val verificationSummaryView = view.findViewById<ConstraintLayout>(R.id.clVerificationSummary)
            val verificationResultView = view.findViewById<ConstraintLayout>(R.id.clVerificationResult)
            val verificationResultValidView = view.findViewById<ConstraintLayout>(R.id.clVerificationResultValid)
            val verificationResultInvalidView = view.findViewById<ConstraintLayout>(R.id.clVerificationResultInvalid)

            verificationSummaryView.isVisible = false
            verificationResultView.isVisible = true
            verificationResultValidView.isVisible = isValid
            verificationResultInvalidView.isVisible = !isValid

            val bottomSheetDialog = if (isValid) {
                BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            } else {
                BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialogRed)
            }

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            bottomSheetDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
