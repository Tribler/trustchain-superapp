package nl.tudelft.trustchain.ssi.ui.dialogs.misc

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputEditText
import nl.tudelft.trustchain.ssi.Communication
import nl.tudelft.trustchain.ssi.R
import nl.tudelft.trustchain.ssi.util.encodeB64

class RendezvousDialog(private val callback: (() -> Unit)? = null) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = it.layoutInflater
            val view = inflater.inflate(R.layout.rendezvous_dialog, null)
            builder.setView(view)
                .setPositiveButton(
                    "Save", null
                )
                .setNegativeButton(
                    R.string.cancel
                ) { _, _ -> }
                .setTitle("Set Rendezvous Token")

            val rendezvousInput = view.findViewById<TextInputEditText>(R.id.rendezvous_input)
            val dialog = builder.create()
            dialog.setOnShowListener {
                val posBtn = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                posBtn.setOnClickListener {
                    val inputValue = rendezvousInput.text.toString()
                    if (inputValue != "") {
                        val token = encodeB64(inputValue.toByteArray())
                        Communication.setActiveRendezvousToken(token)
                        Toast.makeText(
                            requireContext(),
                            "Successfully set token",
                            Toast.LENGTH_LONG
                        ).show()
                        callback?.invoke()
                        dialog.dismiss()
                    } else {
                        rendezvousInput.error = "Enter a token."
                    }
                }
            }
            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
