package nl.tudelft.trustchain.valuetransfer.dialogs

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.core.net.toUri
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.valuetransfer.util.toggleButton
import nl.tudelft.trustchain.peerchat.db.PeerChatStore
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ui.contacts.ContactChatFragment

class ContactRenameDialog(
    private val contact: Contact
) : DialogFragment() {

    private val peerChatStore by lazy {
        PeerChatStore.getInstance(requireContext())
    }

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

            val contactNameView = view.findViewById<EditText>(R.id.etContactName)
            val saveContactNameButton = view.findViewById<Button>(R.id.btnSaveContactName)

            contactNameView.setText(contact.name)

            toggleButton(saveContactNameButton, contact.name.isNotEmpty())

            contactNameView.doAfterTextChanged { state ->
                toggleButton(saveContactNameButton, state != null && state.isNotEmpty())
            }

            saveContactNameButton.setOnClickListener {
                peerChatStore.contactsStore.updateContact(contact.publicKey, contactNameView.text.toString())

                val intent = Intent()
                intent.type = "text/plain"
                intent.data = contactNameView.text.toString().toUri()

                targetFragment!!.onActivityResult(ContactChatFragment.RENAME_CONTACT, Activity.RESULT_OK, intent)

                bottomSheetDialog.dismiss()

            }

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            bottomSheetDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
