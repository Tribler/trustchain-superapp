package nl.tudelft.ipv8.android.demo.ui.bitcoin

import android.app.Dialog
import android.os.Bundle
import android.widget.Switch
import android.widget.TextView
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

                    val pk = view.findViewById<TextView>(R.id.public_key_input)
                    val sk = view.findViewById<TextView>(R.id.private_key_input)
                    val netSwitch = view.findViewById<Switch>(R.id.net_switch)

                    listener.onImport(pk.text.toString(), sk.text.toString(), netSwitch.isChecked)

                    pk.text = ""
                    sk.text = ""
                    netSwitch.isSelected = true

                    listener.onImportDone()
                }
                .setNegativeButton(
                    "Cancel"
                ) { dialog, _ ->
                    dialog.cancel()
                }

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
