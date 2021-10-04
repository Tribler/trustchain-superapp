package nl.tudelft.trustchain.valuetransfer.dialogs

import android.os.Bundle
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import nl.tudelft.trustchain.valuetransfer.util.setNavigationBarColor
import java.lang.IllegalStateException

class IdentityAttestationAuthorityDialog(
    private val authorityKey: PublicKey,
) : VTDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_identity_attestation_authority, null)

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.behavior.apply {
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }

            setNavigationBarColor(requireContext(), parentActivity, bottomSheetDialog)

            val authorityAddressValue = view.findViewById<EditText>(R.id.etAuthorityAddressValue)
            val addAuthorityButton = view.findViewById<Button>(R.id.btnAddAuthority)

            authorityAddressValue.setText(authorityKey.keyToBin().toHex())

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            addAuthorityButton.setOnClickListener {
                getAttestationCommunity().trustedAuthorityManager.let { authorityManager ->
                    when (!authorityManager.contains(authorityKey.keyToHash().toHex())) {
                        true -> {
                            authorityManager.addTrustedAuthority(authorityKey)
                            parentActivity.displaySnackbar(
                                requireContext(),
                                resources.getString(R.string.snackbar_authority_add_success)
                            )
                        }
                        else -> {
                            parentActivity.displaySnackbar(
                                requireContext(),
                                resources.getString(R.string.snackbar_authority_add_error),
                                type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR
                            )
                        }
                    }
                }

                bottomSheetDialog.dismiss()
            }

            bottomSheetDialog
        } ?: throw IllegalStateException(resources.getString(R.string.text_activity_not_null_requirement))
    }
}
