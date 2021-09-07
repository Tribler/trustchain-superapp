package nl.tudelft.trustchain.valuetransfer.dialogs

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import java.lang.IllegalStateException

class IdentityAttestationAuthorityDialog(
    private val authorityKey: PublicKey,
) : DialogFragment() {

    private lateinit var parentActivity: ValueTransferMainActivity
    private lateinit var attestationCommunity: AttestationCommunity

    private lateinit var dialogView: View

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_identity_attestation_authority, null)

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.behavior.skipCollapsed = true
            bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

            parentActivity = requireActivity() as ValueTransferMainActivity
            attestationCommunity = parentActivity.getCommunity(ValueTransferMainActivity.attestationCommunityTag) as AttestationCommunity
            dialogView = view

            val authorityAddressValue = view.findViewById<EditText>(R.id.etAuthorityAddressValue)
            val addAuthorityButton = view.findViewById<Button>(R.id.btnAddAuthority)

            authorityAddressValue.setText(authorityKey.keyToBin().toHex())

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            addAuthorityButton.setOnClickListener {
                val authorityManager = attestationCommunity.trustedAuthorityManager
                when (!authorityManager.contains(authorityKey.keyToHash().toHex())) {
                    true -> {
                        authorityManager.addTrustedAuthority(authorityKey)
                        parentActivity.displaySnackbar(requireContext(), "Authority has been added")
                    }
                    else -> {
                        parentActivity.displaySnackbar(requireContext(), "Authority already been added before", view = view.rootView, type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR, extraPadding = true)
                    }
                }

                bottomSheetDialog.dismiss()
            }

            bottomSheetDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
