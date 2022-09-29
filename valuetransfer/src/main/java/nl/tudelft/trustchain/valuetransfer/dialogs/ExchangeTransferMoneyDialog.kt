package nl.tudelft.trustchain.valuetransfer.dialogs

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ncorti.slidetoact.SlideToActView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.valuetransfer.entity.TransferRequest
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ui.QRScanController
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import nl.tudelft.trustchain.valuetransfer.util.*

class ExchangeTransferMoneyDialog(
    private val recipient: Contact?,
    private val amount: String?,
    private val isTransfer: Boolean,
    private val message: String? = ""
) : VTDialogFragment() {

    private var selectedContact: Contact? = recipient
    private var transactionAmount = 0L
    private var transactionMessage = message ?: ""

    private lateinit var contactAdapter: ArrayAdapter<Contact>

    private lateinit var transferSlider: SlideToActView
    private lateinit var requestSlider: SlideToActView
    private lateinit var requestQRSlider: SlideToActView
    private lateinit var messageView: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_exchange_transfer, null)

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.behavior.apply {
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }

            setNavigationBarColor(requireContext(), parentActivity, bottomSheetDialog)

            val contactSpinner = view.findViewById<Spinner>(R.id.spinnerContact)
            val selectedContactView = view.findViewById<TextView>(R.id.tvSelectedContact)
            val transactionAmountView = view.findViewById<EditText>(R.id.etTransactionAmount)
            messageView = view.findViewById(R.id.etTransactionMessage)

            if (isTransfer && message != "") {
                messageView.setText(resources.getString(R.string.text_re, transactionMessage))
            }

            val recipientTitleView = view.findViewById<TextView>(R.id.tvRecipientTitle)
            val addNewContactView = view.findViewById<ConstraintLayout>(R.id.clAddNewContact)
            val addNewContactSwitch = view.findViewById<Switch>(R.id.switchAddNewContact)

            transferSlider = view.findViewById(R.id.slideTransferMoney)
            transferSlider.isLocked = selectedContact != null && transactionAmount != 0L
            requestSlider = view.findViewById(R.id.slideRequestMoney)
            requestQRSlider = view.findViewById(R.id.slideRequestMoneyQR)

            transactionAmountView.addDecimalLimiter()

            if (amount != null) {
                transactionAmount = amount.toLong()
                transactionAmountView.setText(formatAmount(amount).toString())
            }

            contactSpinner.isVisible = selectedContact == null
            selectedContactView.isVisible = selectedContact != null

            transferSlider.isVisible = isTransfer
            requestSlider.isVisible = !isTransfer && recipient != null
            requestQRSlider.isVisible = !isTransfer && recipient == null
            view.findViewById<TextView>(R.id.tvTitleTransfer).isVisible = isTransfer
            view.findViewById<TextView>(R.id.tvTitleRequest).isVisible = !isTransfer

            parentActivity.getBalance(true).observe(
                this,
                Observer {

                    transactionAmountView.hint = if (isTransfer) resources.getString(R.string.text_balance_max, it) else ""
                }
            )

            // Select contact from dropdown, or nobody in case of an unspecified request
            if (recipient == null) {
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

                    // Add option to select nobody for money request (qrcode only)
                    if (!isTransfer) {
                        contacts.add(
                            0,
                            Contact(
                                resources.getString(R.string.text_no_contact_selected),
                                getTrustChainCommunity().myPeer.publicKey
                            )
                        )
                    }

                    if (!isTransfer || (isTransfer && contacts.isNotEmpty())) {
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
                                return (super.getDropDownView(position, convertView, parent) as TextView).apply {
                                    layoutParams.apply {
                                        height =
                                            resources.getDimensionPixelSize(R.dimen.textViewHeight)
                                    }
                                    gravity = Gravity.CENTER_VERTICAL
                                    text = contacts[position].name

                                    getPeerChatStore().getContactState(contacts[position].publicKey)?.identityInfo?.let { info ->
                                        this.setVerifiedIcon(info.isVerified)
                                    }

                                    // Create a request for an unspecified contact/person
                                    if (!isTransfer && position == 0) {
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

                                    // Currently selected item background in dropdown
                                    background = if (position == contactSpinner.selectedItemPosition) {
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
                                return (super.getView(position, convertView, parent) as TextView).apply {
                                    text = contacts[position].name

                                    getPeerChatStore().getContactState(contacts[position].publicKey)?.identityInfo?.let { info ->
                                        this.setVerifiedIcon(info.isVerified)
                                    }

                                    // No contact selected option for money request
                                    if (!isTransfer && position == 0) {
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
                        contactSpinner.adapter = contactAdapter

                        // Select item in spinner in case the contact is pre-assigned
                        if (selectedContact != null) {
                            contactSpinner.setSelection(contacts.indexOf(selectedContact))
                        }
                    } else {
                        selectedContactView.isVisible = true
                        contactSpinner.isVisible = false
                    }
                }

                contactSpinner.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onNothingSelected(p0: AdapterView<*>?) {
                            transferSlider.isLocked = true
                        }

                        // Get selected contact for transfer and request
                        override fun onItemSelected(
                            p0: AdapterView<*>?,
                            p1: View?,
                            position: Int,
                            p3: Long
                        ) {
                            selectedContact = if (!isTransfer && position == 0) null else contactSpinner.selectedItem as Contact

                            toggleAllSliders()
                        }
                    }

                // Display the recipients name instead of a spinner when the contact is pre-assigned
            } else {
                recipientTitleView.text = resources.getString(R.string.text_recipient)

                // Check whether the contact is already in my contacts
                if (getContactStore().getContactFromPublicKey(selectedContact!!.publicKey) == null) {
                    selectedContactView.text = resources.getString(R.string.text_recipient_not_a_contact, recipient.name)
                    addNewContactView.isVisible = true
                } else {
                    selectedContactView.text = getContactStore().getContactFromPublicKey(selectedContact!!.publicKey)!!.name

                    getPeerChatStore().getContactState(selectedContact!!.publicKey)?.identityInfo?.let { info ->
                        selectedContactView.setVerifiedIcon(info.isVerified)
                    }
                }

                toggleAllSliders()
            }

            onFocusChange(transactionAmountView, requireContext())
            onFocusChange(messageView, requireContext())

            transactionAmountView.doAfterTextChanged {
                transactionAmount = formatAmount(transactionAmountView.text.toString())

                toggleAllSliders()
            }

            messageView.doAfterTextChanged {
                transactionMessage = messageView.text.toString()
            }

            transferSlider.onSlideCompleteListener = object : SlideToActView.OnSlideCompleteListener {
                override fun onSlideComplete(view: SlideToActView) {

                    @Suppress("DEPRECATION")
                    Handler().postDelayed(
                        {
                            try {
                                // Add contact if it is isn't in your contacts and
                                if (selectedContact != null && getContactStore().getContactFromPublicKey(
                                        selectedContact!!.publicKey
                                    ) == null
                                ) {
                                    if (addNewContactSwitch.isChecked) {
                                        getContactStore().addContact(
                                            selectedContact!!.publicKey,
                                            selectedContact!!.name
                                        )
                                    }
                                }

                                // Create proposal block to the recipient
                                val block = getTransactionRepository().sendTransferProposalSync(
                                    selectedContact!!.publicKey.keyToBin(),
                                    transactionAmount
                                )
                                if (block == null) {
                                    parentActivity.displayToast(
                                        requireContext(),
                                        resources.getString(R.string.snackbar_insufficient_balance)
                                    )
                                    transferSlider.text = resources.getString(R.string.btn_slide_to_transfer)
                                    transferSlider.resetSlider()
                                } else {
                                    getPeerChatCommunity().sendMessageWithTransaction(
                                        transactionMessage,
                                        block.calculateHash(),
                                        selectedContact!!.publicKey,
                                        getIdentityCommunity().getIdentityInfo(appPreferences.getIdentityFaceHash())
                                    )
                                    parentActivity.displayToast(
                                        requireContext(),
                                        resources.getString(R.string.snackbar_transfer_of, transactionAmountView.text, selectedContact!!.name),
                                        isShort = false
                                    )
                                    bottomSheetDialog.dismiss()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                parentActivity.displayToast(
                                    requireContext(),
                                    resources.getString(R.string.snackbar_unexpected_error_occurred)
                                )
                            }
                        },
                        500
                    )
                }
            }

            requestSlider.onSlideCompleteListener = object : SlideToActView.OnSlideCompleteListener {
                override fun onSlideComplete(view: SlideToActView) {
                    requestSlider.text = resources.getString(R.string.text_requesting)

                    val transferRequest = TransferRequest(
                        transactionMessage,
                        transactionAmount,
                        getTrustChainCommunity().myPeer.publicKey,
                        selectedContact!!.publicKey
                    )

                    @Suppress("DEPRECATION")
                    Handler().postDelayed(
                        {
                            requestSlider.isLocked = true
                            getPeerChatCommunity().sendTransferRequest(
                                transactionMessage,
                                transferRequest,
                                selectedContact!!.publicKey,
                                getIdentityCommunity().getIdentityInfo(appPreferences.getIdentityFaceHash())
                            )

                            // Only show snackbar when not sent from chat
                            if (recipient == null) {
                                parentActivity.displayToast(
                                    requireContext(),
                                    resources.getString(
                                        R.string.snackbar_transfer_request,
                                        transactionAmountView.text,
                                        selectedContact!!.name
                                    ),
                                    isShort = false
                                )
                            }
                            bottomSheetDialog.dismiss()
                        },
                        500
                    )
                }
            }

            requestQRSlider.onSlideCompleteListener = object : SlideToActView.OnSlideCompleteListener {
                override fun onSlideComplete(view: SlideToActView) {
                    view.closeKeyboard(requireContext())

                    val map = mapOf(
                        QRScanController.KEY_PUBLIC_KEY to getTrustChainCommunity().myPeer.publicKey.keyToBin()
                            .toHex(),
                        QRScanController.KEY_AMOUNT to transactionAmount.toString(),
                        QRScanController.KEY_NAME to getIdentityStore().getIdentity()!!.content.let {
                            "${it.givenNames.getInitials()} ${it.surname}"
                        },
                        QRScanController.KEY_TYPE to QRScanController.VALUE_TRANSFER,
                        QRScanController.KEY_MESSAGE to transactionMessage
                    )

                    bottomSheetDialog.dismiss()
                    QRCodeDialog(
                        resources.getString(R.string.text_request_money),
                        resources.getString(R.string.text_scan_qr_friend),
                        mapToJSON(map).toString()
                    ).show(parentFragmentManager, tag)
                }
            }

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            bottomSheetDialog
        } ?: throw IllegalStateException(resources.getString(R.string.text_activity_not_null_requirement))
    }

    fun TextView.setVerifiedIcon(isVerified: Boolean) {
        if (isVerified) {
            R.drawable.ic_verified_smaller
        } else {
            R.drawable.ic_verified_not_smaller
        }.let { drawable ->
            this.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, drawable, 0)
        }
    }

    private fun toggleAllSliders() {
        val nonEmptyContact = selectedContact != null
        val nonEmptyAmount = transactionAmount != 0L

        transferSlider.isVisible = isTransfer

        if (isTransfer) {
            transferSlider.isLocked = !(nonEmptyContact && nonEmptyAmount)
        } else {
            // Situation in which money is requested from one person
            requestSlider.isVisible = nonEmptyContact
            requestSlider.isLocked = !(nonEmptyContact && nonEmptyAmount)

            // Situation in which money is requested to anyone using QR code only
            requestQRSlider.isVisible = !nonEmptyContact
            requestQRSlider.isLocked = !(!nonEmptyContact && nonEmptyAmount)
        }
    }

    companion object {
        const val TAG = "exchange_transfer_money_dialog"
    }
}
