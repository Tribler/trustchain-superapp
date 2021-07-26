package nl.tudelft.trustchain.valuetransfer.dialogs

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.eurotoken.ui.transfer.TransferFragment.Companion.getAmount
import nl.tudelft.trustchain.peerchat.community.PeerChatCommunity
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.db.IdentityStore
import nl.tudelft.trustchain.valuetransfer.util.*


class TransferMoneyDialog(
    private val recipient: Contact?,
    private val isTransfer: Boolean,
    private val transactionRepository: TransactionRepository,
    private val getPeerChatCommunity: PeerChatCommunity
) : DialogFragment() {

    private val gatewayStore by lazy {
        GatewayStore.getInstance(requireContext())
    }

    private val contactStore by lazy {
        ContactStore.getInstance(requireContext())
    }

    private var selectedContact: Contact? = recipient
    private var transactionAmount = 0L
    private var transactionMessage = ""

    private lateinit var contactAdapter: ArrayAdapter<Contact>

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_exchange_transfer, null)

            val contactSpinner = view.findViewById<Spinner>(R.id.spinnerContact)
            val transactionAmountView = view.findViewById<EditText>(R.id.etTransactionAmount)
            val messageView = view.findViewById<EditText>(R.id.etTransactionMessage)

            val transferButton = view.findViewById<Button>(R.id.btnTransferMoney)
            toggleButton(transferButton, selectedContact != null && transactionAmount != 0L)

            val requestMoneyContactButton = view.findViewById<Button>(R.id.btnRequestMoneyContact)
            val requestMoneyQRButton = view.findViewById<Button>(R.id.btnRequestMoneyQR)

            requestMoneyContactButton.isVisible = !isTransfer
            requestMoneyQRButton.isVisible = !isTransfer
            transferButton.isVisible = isTransfer
            view.findViewById<TextView>(R.id.tvTitleTransfer).isVisible = isTransfer
            view.findViewById<TextView>(R.id.tvTitleRequest).isVisible = !isTransfer


            if(isTransfer) {

            }else{

            }

            transactionAmountView.addDecimalLimiter()

            lifecycleScope.launch(Dispatchers.Main) {
                var contacts : MutableList<Contact> = contactStore.getContacts()
                    .first()
                    .filter {
                        it.publicKey != transactionRepository.trustChainCommunity.myPeer.publicKey
                    }
                    .sortedBy {
                        it.name
                    }
                    .toMutableList()

                if(!isTransfer) {
                    contacts.add(
                        0,
                        Contact(
                            "No contact selected",
                            transactionRepository.trustChainCommunity.myPeer.publicKey
                        )
                    )
                }

                contactAdapter = object : ArrayAdapter<Contact>(requireContext(), android.R.layout.simple_spinner_item, contacts) {
                    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val textView: TextView = super.getDropDownView(position, convertView, parent) as TextView

                        textView.setPadding(30)
                        textView.text = contacts[position].name

                        if (!isTransfer && position == 0) {
                            textView.setTextColor(ContextCompat.getColor(this.context, R.color.dark_gray))
                            textView.setTypeface(null, Typeface.ITALIC)
                        }

                        if (position == contactSpinner.selectedItemPosition){
                            textView.background = ColorDrawable(Color.LTGRAY)
                        }

                        return textView
                    }

                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val textView: TextView = super.getView(position, convertView, parent) as TextView
                        textView.text = contacts[position].name

                        if (!isTransfer && position == 0) {
                            textView.setTextColor(ContextCompat.getColor(this.context, R.color.dark_gray))
                            textView.setTypeface(null, Typeface.ITALIC)
                        }

                        return textView
                    }
                }
                contactSpinner.adapter = contactAdapter

                if(selectedContact != null) {
                    val selected = contacts.indexOf(selectedContact)
                    contactSpinner.setSelection(selected)
                }
            }

            contactSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(p0: AdapterView<*>?) {
                    toggleButton(transferButton, false)
                }

                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                    selectedContact = if(!isTransfer && position == 0) {
                        null
                    }else{
                        contactSpinner.selectedItem as Contact
                    }
                    if(isTransfer) {
                        toggleButton(transferButton, selectedContact != null && transactionAmount != 0L)
                    }else {
                        requestMoneyContactButton.isVisible = (position != 0)
                        toggleButton(requestMoneyContactButton, selectedContact != null && transactionAmount != 0L)
                        requestMoneyQRButton.isVisible = (position == 0)
                        toggleButton(requestMoneyQRButton, selectedContact == null && transactionAmount != 0L)
                    }

                }
            }

            onFocusChange(transactionAmountView, requireContext())
            onFocusChange(messageView, requireContext())

            transactionAmountView.doAfterTextChanged {
                transactionAmount = getAmount(transactionAmountView.text.toString())
                if(isTransfer) {
                    toggleButton(transferButton, transactionAmount != 0L && selectedContact != null)
                }else{
                    toggleButton(requestMoneyContactButton, selectedContact != null && transactionAmount != 0L)
                    toggleButton(requestMoneyQRButton, selectedContact == null && transactionAmount != 0L)
                }

            }

            messageView.doAfterTextChanged {
                transactionMessage = messageView.text.toString()
            }

            transferButton.setOnClickListener {
                try {
//                    CoroutineScope(Dispatchers.IO).launch {
                        val block = transactionRepository.sendTransferProposalSync(selectedContact!!.publicKey.keyToBin(), transactionAmount)
                        if (block == null) {
                            Toast.makeText(requireContext(), "Insufficient balance", Toast.LENGTH_LONG).show()
                        } else {
                            getPeerChatCommunity.sendMessageWithTransaction(transactionMessage, block.calculateHash(), selectedContact!!.publicKey)
                            Toast.makeText(requireContext(), "TRANSFER OF ${transactionAmountView.text} to ${selectedContact!!.name}", Toast.LENGTH_LONG).show()
                            bottomSheetDialog.dismiss()
                        }
//                    }
                } catch(e: Exception) {
                    Toast.makeText(requireContext(), "Unexpected error occurred", Toast.LENGTH_SHORT).show()
                }
            }

            requestMoneyContactButton.setOnClickListener {
                Log.d("TESTJE", "REQUEST TO CONTACT TODO")
            }

            requestMoneyQRButton.setOnClickListener {
                Log.d("TESTJE", "REQUEST QR CODE")

                closeKeyboard(requireContext(), view)
                val map = mapOf(
                    "public_key" to transactionRepository.trustChainCommunity.myPeer.publicKey.keyToBin().toHex(),
                    "amount" to transactionAmount.toString(),
                    "name" to IdentityStore.getInstance(requireContext()).getIdentity()!!.content.givenNames,
                    "type" to "transfer"
                )

                bottomSheetDialog.dismiss()
                QRCodeDialog(
                    "REQUEST MONEY",
                    "Have your friend scan this QR-code",
                    mapToJSON(map).toString()
                )
                    .show(parentFragmentManager,tag)
            }



//            val contactAdapter = object: ArrayAdapter<Contact>(requireContext(), android.R.layout.simple_spinner_item, contacts) {
//                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
//                    val label = super.getView(position, convertView, parent) as TextView
//
//                    label.setText(values[position].getName())
//                    return label
//                }
//                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
//                    val textView: TextView = super.getDropDownView(position, convertView, parent) as TextView
//
//                    textView.setPadding(20)
//
//                    if (position == contactSpinner.selectedItemPosition){
//                        textView.background = ColorDrawable(Color.LTGRAY)
//                    }
//
//                    return textView
//                }
//            }

//            attributeNameSpinner.adapter = attributeNameAdapter
//            attributeNameSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
//                override fun onNothingSelected(parent: AdapterView<*>?) {
//                    selectedName = ""
//                }
//
//                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
//                    selectedName = unusedAttributes[position]
//                    toggleButton(saveButton, selectedName != "" && attributeValueView.text.isNotEmpty())
//                }
//            }




//
//
//
//
//            myPublicKeyTextView.setText(myPublicKey.keyToBin().toHex())
//
//
//
//            contactNameView.doAfterTextChanged {
//                toggleButton(addContactView, contactNameView.text.toString().isNotEmpty() && contactPublicKeyView.text.toString().isNotEmpty())
////                toggleButton(addContactView, contactNameView, contactPublicKeyView)
//            }
//
//            contactPublicKeyView.doAfterTextChanged {
//                toggleButton(addContactView, contactNameView.text.toString().isNotEmpty() && contactPublicKeyView.text.toString().isNotEmpty())
////                toggleButton(addContactView, contactNameView, contactPublicKeyView)
//            }
//
//            copyMyPublicKeyView.setOnClickListener {
//                copyToClipboard(requireContext(),myPublicKey.keyToBin().toHex(), "Public Key")
//            }
//
//            scanContactPublicKeyView.setOnClickListener {
//                QRCodeUtils(requireContext()).startQRScanner(this, vertical = true)
//            }
//
//            addContactView.setOnClickListener {
//                val publicKeyBin = contactPublicKeyView.text.toString()
//                try {
//                    defaultCryptoProvider.keyFromPublicBin(publicKeyBin.hexToBytes())
//                } catch (e: Exception) {
//                    Toast.makeText(requireContext(), "Public key invalid", Toast.LENGTH_SHORT)
//                        .show()
//                } finally {
//                    val publicKey = defaultCryptoProvider.keyFromPublicBin(publicKeyBin.hexToBytes())
//                    val contactName = contactNameView.text.toString()
//
//                    when {
//                        publicKeyBin == myPublicKey.keyToBin().toHex() -> {
//                            Toast.makeText(requireContext(), "You can't add yourself", Toast.LENGTH_SHORT).show()
//                        }
//                        ContactStore.getInstance(requireContext())
//                            .getContactFromPublicKey(publicKey) != null -> {
//                            Toast.makeText(requireContext(), "Contact already added", Toast.LENGTH_SHORT).show()
//                        }
//                        else -> {
//                            ContactStore.getInstance(requireContext()).addContact(publicKey, contactName)
//                            Toast.makeText(requireContext(), "Contact $contactName added", Toast.LENGTH_SHORT).show()
//                            bottomSheetDialog.dismiss()
//                        }
//                    }
//                }
//            }
//
//            contactPublicKey.observe(
//                this,
//                Observer { publicKeyBin ->
//                    contactPublicKeyView.setText(publicKeyBin)
//                }
//            )

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

//            Handler().postDelayed(
//                Runnable {
//                    view.findViewById<ProgressBar>(R.id.pbLoadingSpinner).visibility = View.GONE
//                    myPublicKeyImageView.setImageBitmap(
//                        createBitmap(
//                            requireContext(),
//                            myPublicKey.keyToBin().toHex(),
//                            R.color.black,
//                            R.color.colorPrimaryValueTransfer
//                        )
//                    )
//                }, 100
//            )

            bottomSheetDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
