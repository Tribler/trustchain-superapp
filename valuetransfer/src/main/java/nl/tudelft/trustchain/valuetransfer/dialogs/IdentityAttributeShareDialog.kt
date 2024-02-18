package nl.tudelft.trustchain.valuetransfer.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.common.valuetransfer.entity.IdentityAttribute
import nl.tudelft.trustchain.valuetransfer.databinding.DialogIdentityAttributeShareBinding
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import nl.tudelft.trustchain.valuetransfer.util.*

class IdentityAttributeShareDialog(
    private val recipient: Contact?,
    private val identityAttribute: IdentityAttribute?,
) : VTDialogFragment() {

    private var selectedContact: Contact? = recipient
    private var selectedAttribute: IdentityAttribute? = identityAttribute

    private lateinit var contactAdapter: ArrayAdapter<Contact>
    private lateinit var attributeAdapter: ArrayAdapter<IdentityAttribute>

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog =
                BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val binding = DialogIdentityAttributeShareBinding.inflate(layoutInflater)
            val view = binding.root

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.behavior.apply {
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }

            setNavigationBarColor(requireContext(), parentActivity, bottomSheetDialog)

            val contactSpinner = binding.spinnerContact
            val attributeSpinner = binding.spinnerAttribute
            val selectedContactView = binding.tvSelectedContact
            val recipientTitleView = binding.tvRecipientTitle
            val attributeTitleView = binding.tvSelectedAttributeTitle
            val selectedAttributeView = binding.tvSelectedAttribute

            val shareAttributeButton = binding.btnShareAttribute
            toggleButton(shareAttributeButton, selectedContact != null && selectedAttribute != null)

            contactSpinner.isVisible = selectedContact == null

            var identityAttributes: List<IdentityAttribute>

            lifecycleScope.launch(Dispatchers.Main) {
                identityAttributes = getIdentityStore().getAllAttributes().first().toList()

                // If the user has no identity attributes yet
                if (identityAttributes.isNotEmpty()) {
                    if (identityAttribute == null) {
                        selectedAttributeView.isVisible = false

                        attributeAdapter = object : ArrayAdapter<IdentityAttribute>(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            identityAttributes
                        ) {
                            override fun getDropDownView(
                                position: Int,
                                convertView: View?,
                                parent: ViewGroup
                            ): View {
                                return (super.getDropDownView(
                                    position,
                                    convertView,
                                    parent
                                ) as TextView).apply {
                                    layoutParams.apply {
                                        height =
                                            resources.getDimensionPixelSize(R.dimen.textViewHeight)
                                    }
                                    gravity = Gravity.CENTER_VERTICAL
                                    text = identityAttributes[position].name
                                    setTextColor(
                                        ContextCompat.getColor(
                                            requireContext(),
                                            R.color.black
                                        )
                                    )

                                    // Currently selected item background in dropdown
                                    background =
                                        if (position == attributeSpinner.selectedItemPosition) {
                                            ColorDrawable(Color.LTGRAY)
                                        } else {
                                            ColorDrawable(Color.WHITE)
                                        }
                                }
                            }

                            override fun getView(
                                position: Int,
                                convertView: View?,
                                parent: ViewGroup
                            ): View {
                                return (super.getView(
                                    position,
                                    convertView,
                                    parent
                                ) as TextView).apply {
                                    text = identityAttributes[position].name
                                    setTextColor(
                                        ContextCompat.getColor(
                                            requireContext(),
                                            R.color.black
                                        )
                                    )
                                }
                            }
                        }

                        attributeSpinner.adapter = attributeAdapter

                        attributeSpinner.onItemSelectedListener =
                            object : AdapterView.OnItemSelectedListener {
                                override fun onNothingSelected(p0: AdapterView<*>?) {
                                    toggleButton(shareAttributeButton, false)
                                }

                                // Get selected contact for transfer and request
                                override fun onItemSelected(
                                    p0: AdapterView<*>?,
                                    p1: View?,
                                    position: Int,
                                    p3: Long
                                ) {
                                    selectedAttribute =
                                        attributeSpinner.selectedItem as IdentityAttribute

                                    toggleButton(
                                        shareAttributeButton,
                                        selectedContact != null && selectedAttribute != null
                                    )
                                }
                            }

                        // Select item in spinner in case the identity attribute is pre-assigned
                        if (selectedAttribute != null) {
                            attributeSpinner.setSelection(
                                identityAttributes.indexOf(
                                    selectedAttribute
                                )
                            )
                        } else {
                            if (identityAttributes.isNotEmpty()) {
                                selectedAttribute = identityAttributes[0]
                            }
                        }
                    } else {
                        attributeTitleView.text =
                            resources.getString(R.string.text_selected_attribute)
                        attributeSpinner.isVisible = false
                        selectedAttributeView.text = selectedAttribute!!.name
                    }
                }
            }

            lifecycleScope.launch(Dispatchers.Main) {
                val contacts: List<Contact> = getContactStore().getContacts()
                    .first()
                    .filter {
                        it.publicKey != getTrustChainCommunity().myPeer.publicKey
                    }
                    .sortedBy {
                        it.name
                    }
                    .toList()

                if (contacts.isNotEmpty()) {
                    if (recipient == null) {
                        selectedContactView.isVisible = false

                        contactAdapter = object : ArrayAdapter<Contact>(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            contacts
                        ) {
                            override fun getDropDownView(
                                position: Int,
                                convertView: View?,
                                parent: ViewGroup
                            ): View {
                                return (super.getDropDownView(
                                    position,
                                    convertView,
                                    parent
                                ) as TextView).apply {
                                    layoutParams.apply {
                                        height =
                                            resources.getDimensionPixelSize(R.dimen.textViewHeight)
                                    }
                                    gravity = Gravity.CENTER_VERTICAL
                                    text = contacts[position].name
                                    setTextColor(
                                        ContextCompat.getColor(
                                            requireContext(),
                                            R.color.black
                                        )
                                    )

                                    // Currently selected item background in dropdown
                                    background =
                                        if (position == contactSpinner.selectedItemPosition) {
                                            ColorDrawable(Color.LTGRAY)
                                        } else {
                                            ColorDrawable(Color.WHITE)
                                        }
                                }
                            }

                            override fun getView(
                                position: Int,
                                convertView: View?,
                                parent: ViewGroup
                            ): View {
                                return (super.getView(
                                    position,
                                    convertView,
                                    parent
                                ) as TextView).apply {
                                    text = contacts[position].name
                                    setTextColor(
                                        ContextCompat.getColor(
                                            requireContext(),
                                            R.color.black
                                        )
                                    )
                                }
                            }
                        }
                        contactSpinner.adapter = contactAdapter

                        contactSpinner.onItemSelectedListener =
                            object : AdapterView.OnItemSelectedListener {
                                override fun onNothingSelected(p0: AdapterView<*>?) {
                                    toggleButton(shareAttributeButton, false)
                                }

                                // Get selected contact for transfer and request
                                override fun onItemSelected(
                                    p0: AdapterView<*>?,
                                    p1: View?,
                                    position: Int,
                                    p3: Long
                                ) {
                                    selectedContact = contactSpinner.selectedItem as Contact

                                    toggleButton(
                                        shareAttributeButton,
                                        selectedContact != null && selectedAttribute != null
                                    )
                                }
                            }

                        // Select item in spinner in case the contact is pre-assigned
                        if (selectedContact != null) {
                            contactSpinner.setSelection(contacts.indexOf(selectedContact))
                        }
                    } else {
                        recipientTitleView.text =
                            resources.getString(R.string.text_selected_recipient)
                        contactSpinner.isVisible = false
                        selectedContactView.text = selectedContact!!.name
                    }
                }
            }

            toggleButton(shareAttributeButton, selectedContact != null && selectedAttribute != null)

            shareAttributeButton.setOnClickListener {
                try {
                    getPeerChatCommunity().sendIdentityAttribute(
                        selectedAttribute.toString(),
                        selectedAttribute!!,
                        selectedContact!!.publicKey,
                        getIdentityCommunity().getIdentityInfo(appPreferences.getIdentityFaceHash())
                    )

                    // Only send snackbar when it is sent from identity fragment (and not from within chat)
                    if (identityAttribute != null) {
                        parentActivity.displayToast(
                            requireContext(),
                            resources.getString(
                                R.string.snackbar_identity_attribute_share_success,
                                selectedContact!!.name
                            )
                        )
                    }

                    bottomSheetDialog.dismiss()
                } catch (e: Exception) {
                    e.printStackTrace()
                    parentActivity.displayToast(
                        requireContext(),
                        resources.getString(R.string.snackbar_unexpected_error_occurred)
                    )
                }
            }

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            bottomSheetDialog
        }
            ?: throw IllegalStateException(resources.getString(R.string.text_activity_not_null_requirement))
    }
}
