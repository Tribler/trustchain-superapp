package nl.tudelft.trustchain.valuetransfer.dialogs

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.peerchat.community.PeerChatCommunity
import nl.tudelft.trustchain.valuetransfer.util.*
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity

class ContactShareDialog(
    private val contact: Contact?,
    private val recipient: Contact?
) : DialogFragment() {

    private lateinit var parentActivity: ValueTransferMainActivity
    private lateinit var trustChainCommunity: TrustChainCommunity
    private lateinit var peerChatCommunity: PeerChatCommunity
    private lateinit var contactStore: ContactStore
    private lateinit var dialogView: View

    private var selectedContact: Contact? = contact
    private var selectedRecipient: Contact? = recipient

    private lateinit var contactAdapter: ArrayAdapter<Contact>
    private lateinit var recipientAdapter: ArrayAdapter<Contact>

    private lateinit var buttonShareContact: Button

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_contact_share, null)

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.behavior.skipCollapsed = true
            bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

            parentActivity = requireActivity() as ValueTransferMainActivity
            trustChainCommunity = parentActivity.getCommunity(ValueTransferMainActivity.trustChainCommunityTag) as TrustChainCommunity
            peerChatCommunity = parentActivity.getCommunity(ValueTransferMainActivity.peerChatCommunityTag) as PeerChatCommunity
            contactStore = parentActivity.getStore(ValueTransferMainActivity.contactStoreTag) as ContactStore
            dialogView = view

            val spinnerRecipient = view.findViewById<Spinner>(R.id.spinnerSelectRecipient)
            val selectedRecipientView = view.findViewById<TextView>(R.id.tvSelectedRecipient)
            val spinnerContact = view.findViewById<Spinner>(R.id.spinnerSelectContact)
            val selectedContactView = view.findViewById<TextView>(R.id.tvSelectedContact)
            buttonShareContact = view.findViewById<Button>(R.id.btnShareContact)

            lifecycleScope.launch(Dispatchers.Main) {
                var contacts: MutableList<Contact> = contactStore.getContacts()
                    .first()
                    .filter {
                        it.publicKey != trustChainCommunity.myPeer.publicKey
                    }
                    .sortedBy {
                        it.name
                    }
                    .toMutableList()

                if (contacts.size > 1) {
                    contacts.add(0, Contact("No contact selected", trustChainCommunity.myPeer.publicKey))

                    // Adapter for selecting the recipient
                    if (recipient != null) {
                        spinnerRecipient.isVisible = false
                        selectedRecipientView.text = recipient.name
                    } else {
                        selectedRecipientView.isVisible = false
                        recipientAdapter = spinnerAdapter(contacts, spinnerRecipient, true)
                        spinnerRecipient.adapter = recipientAdapter

                        if (selectedRecipient != null) {
                            val selectedPosition = contacts.indexOf(selectedRecipient)
                            spinnerRecipient.setSelection(selectedPosition)
                        }

                        spinnerSelection(spinnerRecipient, true)
                    }

                    // Adapter for selecting the contact to share
                    if (contact != null) {
                        spinnerContact.isVisible = false
                        selectedContactView.text = contact.name
                    } else {
                        selectedContactView.isVisible = false
                        contactAdapter = spinnerAdapter(contacts, spinnerContact, false)
                        spinnerContact.adapter = contactAdapter

                        if (selectedContact != null) {
                            val selectedPosition = contacts.indexOf(selectedContact)
                            spinnerContact.setSelection(selectedPosition)
                        }

                        spinnerSelection(spinnerContact, false)
                    }
                } else {
                    spinnerContact.isVisible = false
                    spinnerRecipient.isVisible = false

                    if (selectedRecipient != null) {
                        selectedRecipientView.text = selectedRecipient!!.name
                    }

                    if (selectedContact != null) {
                        selectedContactView.text = selectedContact!!.name
                    }
                }
            }

            toggleButton(buttonShareContact, selectedRecipient != null && selectedContact != null)

            buttonShareContact.setOnClickListener {
                try {
                    peerChatCommunity.sendContact(selectedContact!!, selectedRecipient!!.publicKey)

                    parentActivity.displaySnackbar(requireContext(), "Contact shared to ${selectedContact!!.name}")

                    bottomSheetDialog.dismiss()
                } catch (e: Exception) {
                    e.printStackTrace()
                    parentActivity.displaySnackbar(requireContext(), "Unexpected error occurred, please try again", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR)
                }
            }

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            bottomSheetDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun spinnerSelection(spinner: Spinner, isRecipient: Boolean) {
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
                toggleButton(buttonShareContact, false)
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                if (isRecipient) {
                    selectedRecipient = if (position == 0) {
                        null
                    } else {
                        spinner.selectedItem as Contact
                    }
                } else {
                    selectedContact = if (position == 0) {
                        null
                    } else {
                        spinner.selectedItem as Contact
                    }
                }

                toggleButton(buttonShareContact, selectedContact != null && selectedRecipient != null)
            }
        }
    }

    private fun spinnerAdapter(list: MutableList<Contact>, spinner: Spinner, isRecipient: Boolean): ArrayAdapter<Contact> {
        val contacts = if (spinner.selectedItemPosition == 0) {
            list
        } else {
            list.filter {
                it != if (isRecipient) selectedContact else selectedRecipient
            }
        }

        return object : ArrayAdapter<Contact>(requireContext(), android.R.layout.simple_spinner_item, contacts) {
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val textView: TextView = super.getDropDownView(position, convertView, parent) as TextView
                val params = textView.layoutParams
                params.height = resources.getDimensionPixelSize(R.dimen.textViewHeight)
                textView.layoutParams = params
                textView.gravity = Gravity.CENTER_VERTICAL

                textView.text = contacts[position].name

                if (position == 0) {
                    textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_gray))
                    textView.setTypeface(null, Typeface.ITALIC)
                }

                if (position == spinner.selectedItemPosition) {
                    textView.background = ColorDrawable(Color.LTGRAY)
                }

                return textView
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val textView: TextView = super.getView(position, convertView, parent) as TextView

                textView.text = contacts[position].name

                if (position == 0) {
                    textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_gray))
                    textView.setTypeface(null, Typeface.ITALIC)
                }

                return textView
            }
        }
    }
}
