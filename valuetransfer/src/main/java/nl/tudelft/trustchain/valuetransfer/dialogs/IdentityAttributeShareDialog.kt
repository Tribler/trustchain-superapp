package nl.tudelft.trustchain.valuetransfer.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.peerchat.community.PeerChatCommunity
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.db.IdentityStore
import nl.tudelft.trustchain.valuetransfer.entity.IdentityAttribute
import nl.tudelft.trustchain.valuetransfer.util.*

class IdentityAttributeShareDialog(
    private val recipient: Contact?,
    private val identityAttribute: IdentityAttribute?,
) : DialogFragment() {

    private val contactStore by lazy {
        ContactStore.getInstance(requireContext())
    }

    private val identityStore by lazy {
        IdentityStore.getInstance(requireContext())
    }

    private var selectedContact: Contact? = recipient
    private var selectedAttribute: IdentityAttribute? = identityAttribute

    private lateinit var contactAdapter: ArrayAdapter<Contact>
    private lateinit var attributeAdapter: ArrayAdapter<IdentityAttribute>

    private lateinit var parentActivity: ValueTransferMainActivity
    private lateinit var peerChatCommunity: PeerChatCommunity
    private lateinit var trustChainCommunity: TrustChainCommunity

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_identity_attribute_share, null)

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.behavior.skipCollapsed = true
            bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

            parentActivity = requireActivity() as ValueTransferMainActivity
            peerChatCommunity = IPv8Android.getInstance().getOverlay()!!
            trustChainCommunity = IPv8Android.getInstance().getOverlay()!!

            val contactSpinner = view.findViewById<Spinner>(R.id.spinnerContact)
            val attributeSpinner = view.findViewById<Spinner>(R.id.spinnerAttribute)
            val selectedContactView = view.findViewById<TextView>(R.id.tvSelectedContact)
            val recipientTitleView = view.findViewById<TextView>(R.id.tvRecipientTitle)
            val attributeTitleView = view.findViewById<TextView>(R.id.tvSelectedAttributeTitle)
            val selectedAttributeView = view.findViewById<TextView>(R.id.tvSelectedAttribute)

            val shareAttributeButton = view.findViewById<Button>(R.id.btnShareAttribute)
            toggleButton(shareAttributeButton, selectedContact != null && selectedAttribute != null)

            contactSpinner.isVisible = selectedContact == null

            var identityAttributes: List<IdentityAttribute>

            lifecycleScope.launch(Dispatchers.Main) {
                identityAttributes = identityStore.getAllAttributes().first().toList()

                // If the user has no identity attributes yet
                if(identityAttributes.isNotEmpty()) {
                    if(identityAttribute == null) {
                        selectedAttributeView.isVisible = false

                        attributeAdapter = object : ArrayAdapter<IdentityAttribute>(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            identityAttributes
                        ) {
                            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                                val textView: TextView = super.getDropDownView(position, convertView, parent) as TextView
                                val params = textView.layoutParams
                                params.height = resources.getDimensionPixelSize(R.dimen.textViewHeight)
                                textView.layoutParams = params
                                textView.gravity = Gravity.CENTER_VERTICAL

                                textView.text = identityAttributes[position].name

                                // Currently selected item background in dropdown
                                if (position == attributeSpinner.selectedItemPosition) {
                                    textView.background = ColorDrawable(Color.LTGRAY)
                                }

                                return textView
                            }

                            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                                val textView: TextView = super.getView(position, convertView, parent) as TextView
                                textView.text = identityAttributes[position].name

                                return textView
                            }
                        }

                        attributeSpinner.adapter = attributeAdapter

                        attributeSpinner.onItemSelectedListener =
                            object : AdapterView.OnItemSelectedListener {
                                override fun onNothingSelected(p0: AdapterView<*>?) {
                                    toggleButton(shareAttributeButton, false)
                                }

                                // Get selected contact for transfer and request
                                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                                    selectedAttribute = attributeSpinner.selectedItem as IdentityAttribute

                                    toggleButton(shareAttributeButton, selectedContact != null && selectedAttribute != null)
                                }
                            }

                        // Select item in spinner in case the identity attribute is pre-assigned
                        if (selectedAttribute != null) {
                            val selected = identityAttributes.indexOf(selectedAttribute)
                            attributeSpinner.setSelection(selected)
                        } else {
                            if (identityAttributes.isNotEmpty()) {
                                selectedAttribute = identityAttributes[0]
                            }
                        }
                    }else{
                        attributeTitleView.text = "Selected attribute"
                        attributeSpinner.isVisible = false
                        selectedAttributeView.text = selectedAttribute!!.name
                    }
                }
            }

            lifecycleScope.launch(Dispatchers.Main) {
                val contacts: List<Contact> = contactStore.getContacts()
                    .first()
                    .filter {
                        it.publicKey != trustChainCommunity.myPeer.publicKey
                    }
                    .sortedBy {
                        it.name
                    }
                    .toList()

                if (contacts.isNotEmpty()) {
                    if(recipient == null) {
                        selectedContactView.isVisible = false

                        contactAdapter = object : ArrayAdapter<Contact>(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            contacts
                        ) {
                            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                                val textView: TextView = super.getDropDownView(position, convertView, parent) as TextView
                                val params = textView.layoutParams
                                params.height = resources.getDimensionPixelSize(R.dimen.textViewHeight)
                                textView.layoutParams = params
                                textView.gravity = Gravity.CENTER_VERTICAL

                                textView.text = contacts[position].name

                                // Currently selected item background in dropdown
                                if (position == contactSpinner.selectedItemPosition) {
                                    textView.background = ColorDrawable(Color.LTGRAY)
                                }

                                return textView
                            }

                            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                                val textView: TextView = super.getView(position, convertView, parent) as TextView
                                textView.text = contacts[position].name

                                return textView
                            }
                        }
                        contactSpinner.adapter = contactAdapter

                        contactSpinner.onItemSelectedListener =
                            object : AdapterView.OnItemSelectedListener {
                                override fun onNothingSelected(p0: AdapterView<*>?) {
                                    toggleButton(shareAttributeButton, false)
                                }

                                // Get selected contact for transfer and request
                                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                                    selectedContact = contactSpinner.selectedItem as Contact

                                    toggleButton(shareAttributeButton, selectedContact != null && selectedAttribute != null)
                                }
                            }

                        // Select item in spinner in case the contact is pre-assigned
                        if (selectedContact != null) {
                            val selected = contacts.indexOf(selectedContact)
                            contactSpinner.setSelection(selected)
                        }
                    }else{
                        recipientTitleView.text = "Selected recipient"
                        contactSpinner.isVisible = false
                        selectedContactView.text = selectedContact!!.name
                    }
                }
            }

            toggleButton(shareAttributeButton, selectedContact != null && selectedAttribute != null)

            shareAttributeButton.setOnClickListener {
                try {
                    val serializedAttribute = selectedAttribute!!.serialize()

                    peerChatCommunity.sendIdentityAttribute(serializedAttribute, selectedContact!!.publicKey)

                    if(identityAttribute != null) {
                        parentActivity.displaySnackbar(requireContext(), "Identity attribute shared to ${selectedContact!!.name}")
                    }

                    bottomSheetDialog.dismiss()
                } catch(e: Exception) {
                    e.printStackTrace()
                    parentActivity.displaySnackbar(requireContext(), "Unexpected error occurred, please try again", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR)
                }
            }

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            bottomSheetDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
