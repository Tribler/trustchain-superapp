package nl.tudelft.trustchain.valuetransfer.dialogs

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.*
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.util.*
import nl.tudelft.trustchain.valuetransfer.util.*
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.db.IdentityStore
import org.json.JSONObject

class ContactAddDialog(
    private val myPublicKey: PublicKey,
    private val peerPublicKey: PublicKey?,
    private val name: String?
) : DialogFragment() {

    private lateinit var parentActivity: ValueTransferMainActivity
    private lateinit var contactStore: ContactStore
    private lateinit var dialogView: View

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_contact_add, null)

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.behavior.skipCollapsed = true
            bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

            parentActivity = requireActivity() as ValueTransferMainActivity
            contactStore = parentActivity.getStore(ValueTransferMainActivity.contactStoreTag) as ContactStore
            dialogView = view

            val myPublicKeyTextView = view.findViewById<EditText>(R.id.etMyPublicKey)
            val myPublicKeyImageView = view.findViewById<ImageView>(R.id.ivMyQRCode)
            val copyMyPublicKeyView = view.findViewById<ImageView>(R.id.ivCopyMyPublicKey)
            val scanContactPublicKeyView = view.findViewById<ImageView>(R.id.btnScanContactPublicKey)
            val contactNameView = view.findViewById<EditText>(R.id.etContactName)
            val contactPublicKeyView = view.findViewById<EditText>(R.id.etContactPublicKey)
            val addContactView = view.findViewById<Button>(R.id.btnAddContact)

            myPublicKeyTextView.setText(myPublicKey.keyToBin().toHex())
            contactPublicKeyView.setText(peerPublicKey?.keyToBin()?.toHex())
            contactNameView.setText(name)
            toggleButton(addContactView, contactNameView.text.toString().isNotEmpty() && contactPublicKeyView.text.toString().isNotEmpty())

            onFocusChange(contactNameView, requireContext())
            onFocusChange(contactPublicKeyView, requireContext())

            contactNameView.doAfterTextChanged {
                toggleButton(addContactView, contactNameView.text.toString().isNotEmpty() && contactPublicKeyView.text.toString().isNotEmpty())
            }

            contactPublicKeyView.doAfterTextChanged {
                toggleButton(addContactView, contactNameView.text.toString().isNotEmpty() && contactPublicKeyView.text.toString().isNotEmpty())
            }

            copyMyPublicKeyView.setOnClickListener {
                copyToClipboard(requireContext(),myPublicKey.keyToBin().toHex(), "Public key")
                parentActivity.displaySnackbar(requireContext(), "Public key has been copied")
            }

            scanContactPublicKeyView.setOnClickListener {
                QRCodeUtils(requireContext()).startQRScanner(this, promptText = "Scan QR Code to import contact", vertical = true)
            }

            addContactView.setOnClickListener {
                val publicKeyBin = contactPublicKeyView.text.toString()
                try {
                    defaultCryptoProvider.keyFromPublicBin(publicKeyBin.hexToBytes()).let { publicKey ->
                        val contactName = contactNameView.text.toString()

                        when {
                            publicKeyBin == myPublicKey.keyToBin().toHex() -> {
                                parentActivity.displaySnackbar(requireContext(), "You can't add yourself as contact", view = view.rootView, type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR, extraPadding = true)
                            }
                            contactStore.getContactFromPublicKey(publicKey) != null -> {
                                parentActivity.displaySnackbar(requireContext(), "Contact already in address book", view = view.rootView, type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR, extraPadding = true)
                            }
                            else -> {
                                contactStore.addContact(publicKey, contactName)
                                parentActivity.displaySnackbar(requireContext(), "Contact $contactName added to address book")
                                bottomSheetDialog.dismiss()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    parentActivity.displaySnackbar(requireContext(), "Please provide a valid public key", view = view.rootView, type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR, extraPadding = true)
                }
            }

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            Handler().postDelayed(
                Runnable {
                    view.findViewById<ProgressBar>(R.id.pbLoadingSpinner).isVisible = false

                    val map = mapOf(
                        "type" to "contact",
                        "public_key" to IdentityStore.getInstance(requireContext()).getIdentity()!!.publicKey.keyToBin().toHex(),
                        "name" to IdentityStore.getInstance(requireContext()).getIdentity()!!.content.givenNames
                    )

                    myPublicKeyImageView.setImageBitmap(
                        createBitmap(
                            requireContext(),
                            mapToJSON(map).toString(),
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
        QRCodeUtils(requireContext()).parseActivityResult(requestCode, resultCode, data)?.let { result ->
            try {
                val obj = JSONObject(result)

                this.dismiss()

                (requireActivity() as ValueTransferMainActivity).getQRScanController().addContact(obj)
            }catch(e: Exception) {
                e.printStackTrace()
                parentActivity.displaySnackbar(requireContext(), "Scanned QR code not in JSON format", view = dialogView.rootView, type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR, extraPadding = true)
            }
        }
    }
}
