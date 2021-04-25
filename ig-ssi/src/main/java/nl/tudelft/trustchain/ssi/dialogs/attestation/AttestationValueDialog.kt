package nl.tudelft.trustchain.ssi.dialogs.attestation

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Html
import android.text.InputType
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.jaredrummler.blockingdialog.BlockingDialogFragment
import nl.tudelft.ipv8.attestation.schema.ID_METADATA_RANGE_18PLUS
import nl.tudelft.ipv8.attestation.schema.ID_METADATA_RANGE_UNDERAGE
import nl.tudelft.trustchain.ssi.R
import nl.tudelft.trustchain.ssi.parseHtml

@SuppressLint("ValidFragment")
class AttestationValueDialog(private val attributeName: String, private val idFormat: String) :
    BlockingDialogFragment<String>() {

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog? {
        @Suppress("DEPRECATION")
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = it.layoutInflater
            val view = inflater.inflate(R.layout.attestation_value_dialog, null)
            val attrInput = view.findViewById<TextInputEditText>(R.id.value_input)
            when (idFormat) {
                ID_METADATA_RANGE_18PLUS -> attrInput.inputType = InputType.TYPE_CLASS_NUMBER
                ID_METADATA_RANGE_UNDERAGE -> attrInput.inputType = InputType.TYPE_CLASS_NUMBER
                else -> attrInput.inputType = InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE
            }

            builder.setView(view)
                .setPositiveButton(
                    R.string.fire,
                    null
                )
                .setNegativeButton(
                    R.string.cancel,
                    DialogInterface.OnClickListener { _, _ ->
                        setResult("", true)
                    }
                )
                .setTitle("Attestation Requested")
                .setMessage(parseHtml("An attestation has been requested for <b>$attributeName</b> with format <b>$idFormat</b>."))
            // Create the AlertDialog object and return it
            val dialog = builder.create()
            dialog.setOnShowListener {
                val posBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                posBtn.setOnClickListener {
                    val inputValue = attrInput.text.toString()
                    if (inputValue != "") {
                        setResult(inputValue, false)
                        dialog.dismiss()
                    } else {
                        attrInput.error = "Enter a value."
                    }
                }
            }
            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}

class NewAttestationValueDialog(private val attributeName: String, private val idFormat: String, private val callback: (value: String) -> Unit) :
    DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = it.layoutInflater
            val view = inflater.inflate(R.layout.attestation_value_dialog, null)
            val attrInput = view.findViewById<TextInputEditText>(R.id.value_input)
            when (idFormat) {
                ID_METADATA_RANGE_18PLUS -> attrInput.inputType = InputType.TYPE_CLASS_NUMBER
                ID_METADATA_RANGE_UNDERAGE -> attrInput.inputType = InputType.TYPE_CLASS_NUMBER
                else -> attrInput.inputType = InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE
            }


            builder.setView(view)
                .setPositiveButton(
                    R.string.fire,
                    null
                )
                .setNegativeButton(
                    R.string.cancel
                ) { _, _ ->
                    this.dismiss()
                }
                .setTitle("Attestation Requested")
                .setMessage(parseHtml("An attestation has been requested for <b>$attributeName</b> with format <b>$idFormat</b>."))
            // Create the AlertDialog object and return it
            val dialog = builder.create()
            dialog.setOnShowListener {
                val posBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                posBtn.setOnClickListener {
                    val inputValue = attrInput.text.toString()
                    if (inputValue != "") {
                        callback(inputValue)
                        dialog.dismiss()
                    } else {
                        attrInput.error = "Enter a value."
                    }
                }
            }
            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
