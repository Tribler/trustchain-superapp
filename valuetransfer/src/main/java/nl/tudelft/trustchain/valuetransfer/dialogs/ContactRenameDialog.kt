package nl.tudelft.trustchain.valuetransfer.dialogs

import android.content.DialogInterface
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.valuetransfer.util.toggleButton
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import nl.tudelft.trustchain.valuetransfer.util.setNavigationBarColor

class ContactRenameDialog(
    private val contact: Contact,
) : VTDialogFragment() {

    fun newInstance(num: Int): ContactRenameDialog {
        val dialogFragment = ContactRenameDialog(contact)
        val bundle = Bundle()
        bundle.putInt("num", num)
        dialogFragment.arguments = bundle
        return dialogFragment
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_contact_rename, null)

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.behavior.apply {
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }

            val contactNameView = view.findViewById<EditText>(R.id.etContactName)
            val saveContactNameButton = view.findViewById<Button>(R.id.btnSaveContactName)

            setNavigationBarColor(requireContext(), parentActivity, bottomSheetDialog)

            contactNameView.setText(contact.name)

            toggleButton(saveContactNameButton, contact.name.isNotEmpty())

            contactNameView.doAfterTextChanged { state ->
                toggleButton(saveContactNameButton, state != null && state.isNotEmpty())
            }

            saveContactNameButton.setOnClickListener {
                getContactStore().updateContact(contact.publicKey, contactNameView.text.toString())

                val resultBundle = Bundle().apply {
                    putString("name", contactNameView.text.toString())
                }
                setFragmentResult("renameContact", resultBundle)

                if (contact.name.isEmpty()) {
                    resources.getString(R.string.snackbar_contact_add_success, contactNameView.text.toString())
                } else {
                    resources.getString(R.string.snackbar_contact_rename_success, contactNameView.text.toString())
                }.let { text ->
                    bottomSheetDialog.dismiss()
                    parentActivity.displayToast(requireContext(), text)
                    parentActivity.invalidateOptionsMenu()
                }
            }

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            bottomSheetDialog
        } ?: throw IllegalStateException(resources.getString(R.string.text_activity_not_null_requirement))
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        setFragmentResult("renameContact", Bundle())
    }
}
