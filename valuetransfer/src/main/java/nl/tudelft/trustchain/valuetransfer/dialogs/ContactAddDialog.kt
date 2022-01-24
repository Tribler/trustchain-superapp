package nl.tudelft.trustchain.valuetransfer.dialogs

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.*
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.util.*
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ui.QRScanController
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import nl.tudelft.trustchain.valuetransfer.util.*
import org.json.JSONObject

class ContactAddDialog(
    private val myPublicKey: PublicKey,
    private val peerPublicKey: PublicKey?,
    private val name: String?
) : VTDialogFragment() {
    private lateinit var dialogView: View

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_contact_add, null)

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.behavior.apply {
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }

            dialogView = view

            setNavigationBarColor(requireContext(), parentActivity, bottomSheetDialog)

            val myPublicKeyImageView = view.findViewById<ImageView>(R.id.ivMyQRCode)
            val copyMyPublicKeyView = view.findViewById<ImageView>(R.id.ivCopyMyPublicKey)
            val scanContactPublicKeyView = view.findViewById<ImageView>(R.id.btnScanContactPublicKey)
            val contactNameView = view.findViewById<EditText>(R.id.etContactName)
            val contactPublicKeyView = view.findViewById<EditText>(R.id.etContactPublicKey)
            val addContactView = view.findViewById<Button>(R.id.btnAddContact)

            contactPublicKeyView.setText(peerPublicKey?.keyToBin()?.toHex())
            contactNameView.setText(name)
            toggleButton(
                addContactView,
                contactNameView.text.toString().isNotEmpty() && contactPublicKeyView.text.toString().isNotEmpty()
            )

            onFocusChange(contactNameView, requireContext())
            onFocusChange(contactPublicKeyView, requireContext())

            contactNameView.doAfterTextChanged {
                toggleButton(
                    addContactView,
                    contactNameView.text.toString().isNotEmpty() && contactPublicKeyView.text.toString().isNotEmpty()
                )
            }

            contactPublicKeyView.doAfterTextChanged {
                toggleButton(
                    addContactView,
                    contactNameView.text.toString().isNotEmpty() && contactPublicKeyView.text.toString().isNotEmpty()
                )
            }

            copyMyPublicKeyView.setOnClickListener {
                copyToClipboard(
                    requireContext(),
                    myPublicKey.keyToBin().toHex(),
                    resources.getString(R.string.text_public_key)
                )
                parentActivity.displayToast(
                    requireContext(),
                    resources.getString(
                        R.string.snackbar_copied_clipboard,
                        resources.getString(R.string.text_public_key)
                    )
                )
            }

            scanContactPublicKeyView.setOnClickListener {
                QRCodeUtils(requireContext()).startQRScanner(
                    this,
                    promptText = resources.getString(R.string.text_scan_qr_contact_import),
                    vertical = true
                )
            }

            addContactView.setOnClickListener {
                val publicKeyBin = contactPublicKeyView.text.toString()
                try {
                    defaultCryptoProvider.keyFromPublicBin(publicKeyBin.hexToBytes()).let { publicKey ->
                        val contactName = contactNameView.text.toString()

                        when {
                            publicKeyBin == myPublicKey.keyToBin().toHex() -> {
                                parentActivity.displayToast(
                                    requireContext(),
                                    resources.getString(R.string.snackbar_contact_add_error_self)
                                )
                            }
                            getContactStore().getContactFromPublicKey(publicKey) != null -> {
                                parentActivity.displayToast(
                                    requireContext(),
                                    resources.getString(R.string.snackbar_contact_add_error_exists)
                                )
                            }
                            else -> {
                                getContactStore().addContact(publicKey, contactName)
                                parentActivity.displayToast(
                                    requireContext(),
                                    resources.getString(R.string.snackbar_contact_add_success, contactName)
                                )
                                bottomSheetDialog.dismiss()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    parentActivity.displayToast(
                        requireContext(),
                        resources.getString(R.string.snackbar_provide_valid_public_key)
                    )
                }
            }

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            Handler().postDelayed(
                {
                    view.findViewById<ProgressBar>(R.id.pbLoadingSpinner).isVisible = false

                    val map = mapOf(
                        QRScanController.KEY_TYPE to QRScanController.VALUE_CONTACT,
                        QRScanController.KEY_PUBLIC_KEY to getIdentityStore().getIdentity()!!.publicKey.keyToBin().toHex(),
                        QRScanController.KEY_NAME to getIdentityStore().getIdentity()!!.content.let {
                            "${it.givenNames.getInitials()} ${it.surname}"
                        },
                    )

                    myPublicKeyImageView.setImageBitmap(
                        createBitmap(
                            requireContext(),
                            mapToJSON(map).toString(),
                            R.color.black,
                            R.color.light_gray
                        )
                    )
                },
                100
            )

            bottomSheetDialog
        } ?: throw IllegalStateException(resources.getString(R.string.text_activity_not_null_requirement))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        QRCodeUtils(requireContext()).parseActivityResult(requestCode, resultCode, data)?.let { result ->
            try {
                val obj = JSONObject(result)

                this.dismiss()

                parentActivity.getQRScanController().addContact(obj)
            } catch (e: Exception) {
                e.printStackTrace()
                parentActivity.displayToast(
                    requireContext(),
                    resources.getString(R.string.snackbar_qr_code_not_json_format)
                )
            }
        }
    }
}
