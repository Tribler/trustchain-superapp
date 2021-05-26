package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.app.Dialog
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import nl.tudelft.trustchain.currencyii.R

class ImportKeyDialog : DialogFragment() {
    private lateinit var listener: ImportKeyDialogListener

    interface ImportKeyDialogListener {
        fun onImport(address: String, privateKey: String)
        fun onImportDone()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            listener = targetFragment as ImportKeyDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException("Calling Fragment must implement Listener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.import_key_dialog, null)

            builder.setView(view)
                .setTitle("Import Existing Address")
                .setPositiveButton(
                    "Import"
                ) { _, _ ->

                    val ad = view.findViewById<TextView>(R.id.address_input)
                    val sk = view.findViewById<TextView>(R.id.private_key_input)

                    val address = ad.text.toString()
                    val privateKey = sk.text.toString()

                    val addressValid = isAddressValid(address)
                    val privateKeyValid = isPrivateKeyValid(privateKey)

                    if (addressValid && privateKeyValid) {
                        listener.onImport(address, privateKey)
                        ad.text = ""
                        sk.text = ""

                        listener.onImportDone()
                        Toast.makeText(
                            this.requireContext(),
                            "Key imported successfully.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this.requireContext(),
                            "The address or private key is not formatted correctly.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton(
                    "Cancel"
                ) { dialog, _ ->
                    dialog.cancel()
                }

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun isAddressValid(address: String): Boolean {
        return address.length in 26..35
    }

    private fun isPrivateKeyValid(privateKey: String): Boolean {
        return privateKey.length in 51..52 || privateKey.length == 64
    }
}
