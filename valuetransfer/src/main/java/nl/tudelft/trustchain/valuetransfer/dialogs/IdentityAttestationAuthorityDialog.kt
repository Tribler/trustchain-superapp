package nl.tudelft.trustchain.valuetransfer.dialogs

import android.os.Bundle
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.databinding.DialogIdentityAttestationAuthorityBinding
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import nl.tudelft.trustchain.valuetransfer.util.setNavigationBarColor
import java.lang.IllegalStateException

class IdentityAttestationAuthorityDialog(
    private val authorityKey: PublicKey,
) : VTDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val binding = DialogIdentityAttestationAuthorityBinding.inflate(layoutInflater)
            val view = binding.root

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.behavior.apply {
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }

            setNavigationBarColor(requireContext(), parentActivity, bottomSheetDialog)

            val authorityAddressValue = binding.etAuthorityAddressValue
            val addAuthorityButton = binding.btnAddAuthority

            authorityAddressValue.setText(authorityKey.keyToBin().toHex())

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            addAuthorityButton.setOnClickListener {
                getAttestationCommunity().trustedAuthorityManager.let { authorityManager ->
                    when (!authorityManager.contains(authorityKey.keyToHash().toHex())) {
                        true -> {
                            authorityManager.addTrustedAuthority(authorityKey)
                            parentActivity.displayToast(
                                requireContext(),
                                resources.getString(R.string.snackbar_authority_add_success)
                            )
                        }
                        else -> {
                            parentActivity.displayToast(
                                requireContext(),
                                resources.getString(R.string.snackbar_authority_add_error)
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
