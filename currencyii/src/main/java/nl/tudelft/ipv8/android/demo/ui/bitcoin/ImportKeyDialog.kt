package nl.tudelft.ipv8.android.demo.ui.bitcoin

import android.app.Dialog
import android.os.Bundle
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import nl.tudelft.ipv8.android.demo.R

class ImportKeyDialog : DialogFragment() {
    private lateinit var listener: ImportKeyDialogListener

    interface ImportKeyDialogListener {
        fun onImport(address: String, privateKey: String, testNet: Boolean)
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
                    val netSwitch = view.findViewById<Switch>(R.id.net_switch)

                    val address = ad.text.toString()
                    val privateKey = sk.text.toString()

                    val addressValid = isAddressValid(address)
                    val privateKeyValid = isPrivateKeyValid(privateKey)

                    if (addressValid && privateKeyValid) {
                        listener.onImport(address, privateKey, netSwitch.isChecked)
                        ad.text = ""
                        sk.text = ""
                        netSwitch.isSelected = true

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

    fun isAddressValid(address: String): Boolean {
        return address.length in 26..35
    }

    fun isPrivateKeyValid(privateKey: String): Boolean {
        return privateKey.length in 51..52 || privateKey.length == 64
    }

}
