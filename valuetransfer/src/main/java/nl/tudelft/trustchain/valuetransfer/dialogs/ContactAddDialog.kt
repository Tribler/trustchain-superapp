package nl.tudelft.trustchain.valuetransfer.dialogs

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.net.toUri
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.dialog_contact_add.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.util.*
import nl.tudelft.trustchain.valuetransfer.util.*
import nl.tudelft.trustchain.peerchat.db.PeerChatStore
import nl.tudelft.trustchain.peerchat.ui.addcontact.AddContactFragment
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ui.contacts.ContactChatFragment
import kotlin.reflect.jvm.internal.impl.descriptors.Visibilities

class ContactAddDialog(
    private val myPublicKey: PublicKey
) : DialogFragment() {

    private val peerChatStore by lazy {
        PeerChatStore.getInstance(requireContext())
    }

    private var contactPublicKey = MutableLiveData<String>()

//    private fun toggleButton(button: Button, contactName: EditText, contactPublicKey: EditText) {
//        button.isEnabled = contactName.text.toString().isNotEmpty() && contactPublicKey.text.toString().isNotEmpty()
//        button.alpha = if(contactName.text.toString().isNotEmpty() && contactPublicKey.text.toString().isNotEmpty()) 1f else 0.5f
//        button.isClickable = contactName.text.toString().isNotEmpty() && contactPublicKey.text.toString().isNotEmpty()
//    }

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_contact_add, null)

            val myPublicKeyTextView = view.findViewById<EditText>(R.id.etMyPublicKey)
            val myPublicKeyImageView = view.findViewById<ImageView>(R.id.ivMyQRCode)
            val copyMyPublicKeyView = view.findViewById<ImageView>(R.id.ivCopyMyPublicKey)
            val scanContactPublicKeyView = view.findViewById<ImageView>(R.id.btnScanContactPublicKey)
            val contactNameView = view.findViewById<EditText>(R.id.etContactName)
            val contactPublicKeyView = view.findViewById<EditText>(R.id.etContactPublicKey)
            val addContactView = view.findViewById<Button>(R.id.btnAddContact)

            myPublicKeyTextView.setText(myPublicKey.keyToBin().toHex())

            onFocusChange(contactNameView, requireContext())
            onFocusChange(contactPublicKeyView, requireContext())

            contactNameView.doAfterTextChanged {
                toggleButton(addContactView, contactNameView.text.toString().isNotEmpty() && contactPublicKeyView.text.toString().isNotEmpty())
//                toggleButton(addContactView, contactNameView, contactPublicKeyView)
            }

            contactPublicKeyView.doAfterTextChanged {
                toggleButton(addContactView, contactNameView.text.toString().isNotEmpty() && contactPublicKeyView.text.toString().isNotEmpty())
//                toggleButton(addContactView, contactNameView, contactPublicKeyView)
            }

            copyMyPublicKeyView.setOnClickListener {
                copyToClipboard(requireContext(),myPublicKey.keyToBin().toHex(), "Public Key")
            }

            scanContactPublicKeyView.setOnClickListener {
                QRCodeUtils(requireContext()).startQRScanner(this, vertical = true)
            }

            addContactView.setOnClickListener {
                val publicKeyBin = contactPublicKeyView.text.toString()
                try {
                    defaultCryptoProvider.keyFromPublicBin(publicKeyBin.hexToBytes())
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Public key invalid", Toast.LENGTH_SHORT)
                        .show()
                } finally {
                    val publicKey = defaultCryptoProvider.keyFromPublicBin(publicKeyBin.hexToBytes())
                    val contactName = contactNameView.text.toString()

                    when {
                        publicKeyBin == myPublicKey.keyToBin().toHex() -> {
                            Toast.makeText(requireContext(), "You can't add yourself", Toast.LENGTH_SHORT).show()
                        }
                        ContactStore.getInstance(requireContext())
                            .getContactFromPublicKey(publicKey) != null -> {
                            Toast.makeText(requireContext(), "Contact already added", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            ContactStore.getInstance(requireContext()).addContact(publicKey, contactName)
                            Toast.makeText(requireContext(), "Contact $contactName added", Toast.LENGTH_SHORT).show()
                            bottomSheetDialog.dismiss()
                        }
                    }
                }
            }

            contactPublicKey.observe(
                this,
                Observer { publicKeyBin ->
                    contactPublicKeyView.setText(publicKeyBin)
                }
            )

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            Handler().postDelayed(
                Runnable {
                    view.findViewById<ProgressBar>(R.id.pbLoadingSpinner).visibility = View.GONE
                    myPublicKeyImageView.setImageBitmap(
                        createBitmap(
                            requireContext(),
                            myPublicKey.keyToBin().toHex(),
                            R.color.black,
                            R.color.colorPrimaryValueTransfer
                        )
                    )
                }, 100
            )

            bottomSheetDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val publicKeyBin = QRCodeUtils(requireContext()).parseActivityResult(requestCode, resultCode, data)

        if (publicKeyBin != null) {
            try {
                defaultCryptoProvider.keyFromPublicBin(publicKeyBin.hexToBytes())
                contactPublicKey.value = publicKeyBin
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Invalid public key", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(requireContext(), "Scan failed", Toast.LENGTH_LONG).show()
        }
    }
}
