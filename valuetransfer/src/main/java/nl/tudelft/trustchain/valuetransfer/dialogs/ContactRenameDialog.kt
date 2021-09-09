package nl.tudelft.trustchain.valuetransfer.dialogs

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.core.net.toUri
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.valuetransfer.util.toggleButton
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.ui.contacts.ContactChatFragment

class ContactRenameDialog(
    private val contact: Contact,
) : DialogFragment() {

    private lateinit var parentActivity: ValueTransferMainActivity
    private lateinit var contactStore: ContactStore

    fun newInstance(num: Int): ContactRenameDialog? {
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
            bottomSheetDialog.behavior.skipCollapsed = true
            bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

            val contactNameView = view.findViewById<EditText>(R.id.etContactName)
            val saveContactNameButton = view.findViewById<Button>(R.id.btnSaveContactName)

            parentActivity = (requireActivity() as ValueTransferMainActivity)
            contactStore = parentActivity.getStore(ValueTransferMainActivity.contactStoreTag) as ContactStore

            contactNameView.setText(contact.name)

            toggleButton(saveContactNameButton, contact.name.isNotEmpty())

            contactNameView.doAfterTextChanged { state ->
                toggleButton(saveContactNameButton, state != null && state.isNotEmpty())
            }

            saveContactNameButton.setOnClickListener {
                contactStore.updateContact(contact.publicKey, contactNameView.text.toString())

                val intent = Intent()
                intent.type = "text/plain"
                intent.data = contactNameView.text.toString().toUri()

                targetFragment!!.onActivityResult(ContactChatFragment.RENAME_CONTACT, Activity.RESULT_OK, intent)

                bottomSheetDialog.dismiss()

                if (contact.name.isEmpty()) {
                    "Contact has been added"
                } else {
                    "Contact has been renamed"
                }.let { text ->
                    parentActivity.displaySnackbar(requireContext(), text)
                }
            }

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            bottomSheetDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
