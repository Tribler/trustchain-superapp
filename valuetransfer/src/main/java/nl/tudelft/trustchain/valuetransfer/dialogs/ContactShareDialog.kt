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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.valuetransfer.util.*
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment

class ContactShareDialog(
    private val contact: Contact?,
    private val recipient: Contact?
) : VTDialogFragment() {
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
            bottomSheetDialog.behavior.apply {
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }

            dialogView = view

            setNavigationBarColor(requireContext(), parentActivity, bottomSheetDialog)

            val spinnerRecipient = view.findViewById<Spinner>(R.id.spinnerSelectRecipient)
            val selectedRecipientView = view.findViewById<TextView>(R.id.tvSelectedRecipient)
            val spinnerContact = view.findViewById<Spinner>(R.id.spinnerSelectContact)
            val selectedContactView = view.findViewById<TextView>(R.id.tvSelectedContact)
            buttonShareContact = view.findViewById<Button>(R.id.btnShareContact)

            lifecycleScope.launch(Dispatchers.Main) {
                val contacts: MutableList<Contact> = getContactStore().getContacts()
                    .first()
                    .filter {
                        it.publicKey != getTrustChainCommunity().myPeer.publicKey
                    }
                    .sortedBy {
                        it.name
                    }
                    .toMutableList()

                if (contacts.size > 1) {
                    contacts.add(
                        0,
                        Contact(
                            resources.getString(R.string.text_no_contact_selected),
                            getTrustChainCommunity().myPeer.publicKey
                        )
                    )

                    // Adapter for selecting the recipient
                    if (recipient != null) {
                        spinnerRecipient.isVisible = false
                        selectedRecipientView.text = recipient.name
                    } else {
                        selectedRecipientView.isVisible = false
                        recipientAdapter = spinnerAdapter(contacts, spinnerRecipient, true)
                        spinnerRecipient.adapter = recipientAdapter

                        selectedRecipient?.let {
                            spinnerRecipient.setSelection(contacts.indexOf(it))
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

                        selectedContact?.let {
                            spinnerContact.setSelection(contacts.indexOf(it))
                        }

                        spinnerSelection(spinnerContact, false)
                    }
                } else {
                    spinnerContact.isVisible = false
                    spinnerRecipient.isVisible = false

                    selectedRecipient?.let {
                        selectedRecipientView.text = it.name
                    }

                    selectedContact?.let {
                        selectedContactView.text = it.name
                    }
                }
            }

            toggleButton(buttonShareContact, selectedRecipient != null && selectedContact != null)

            buttonShareContact.setOnClickListener {
                try {
                    getPeerChatCommunity().sendContact(
                        selectedContact!!,
                        selectedRecipient!!.publicKey,
                        getIdentityCommunity().getIdentityInfo(appPreferences.getIdentityFaceHash())
                    )

                    // Only show snackbar if contact of chat is shared to another contact (and not some contact that is shared to the contact of the chat)
                    if (recipient != null) {
                        parentActivity.displaySnackbar(
                            requireContext(),
                            resources.getString(
                                R.string.snackbar_contact_shared_to,
                                selectedContact!!.name,
                                selectedRecipient!!.name
                            )
                        )
                    }

                    bottomSheetDialog.dismiss()
                } catch (e: Exception) {
                    e.printStackTrace()
                    parentActivity.displaySnackbar(
                        requireContext(),
                        resources.getString(R.string.snackbar_unexpected_error_occurred),
                        view = view.rootView,
                        type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR
                    )
                }
            }

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            bottomSheetDialog
        } ?: throw IllegalStateException(resources.getString(R.string.text_activity_not_null_requirement))
    }

    private fun spinnerSelection(spinner: Spinner, isRecipient: Boolean) {
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
                toggleButton(buttonShareContact, false)
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                when (isRecipient) {
                    true -> selectedRecipient = if (position == 0) null else spinner.selectedItem as Contact
                    else -> selectedContact = if (position == 0) null else spinner.selectedItem as Contact
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
                return (super.getDropDownView(position, convertView, parent) as TextView).apply {
                    layoutParams.apply {
                        height = resources.getDimensionPixelSize(R.dimen.textViewHeight)
                    }
                    gravity = Gravity.CENTER_VERTICAL
                    text = contacts[position].name

                    if (position == 0) {
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_gray))
                        setTypeface(null, Typeface.ITALIC)
                    } else {
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    }

                    background = if (position == spinner.selectedItemPosition) {
                        ColorDrawable(Color.LTGRAY)
                    } else {
                        ColorDrawable(Color.WHITE)
                    }
                }
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                return (super.getView(position, convertView, parent) as TextView).apply {
                    text = contacts[position].name

                    if (position == 0) {
                        setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.dark_gray
                            )
                        )
                        setTypeface(null, Typeface.ITALIC)
                    } else {
                        setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.black
                            )
                        )
                    }
                }
            }
        }
    }
}
