package nl.tudelft.trustchain.ssi.ui.dialogs.attestation

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.jaredrummler.blockingdialog.BlockingDialogFragment
import nl.tudelft.ipv8.attestation.common.consts.SchemaConstants.ID_METADATA_RANGE_18PLUS // OK
import nl.tudelft.ipv8.attestation.common.consts.SchemaConstants.ID_METADATA_RANGE_UNDERAGE // OK
import nl.tudelft.trustchain.ssi.R
import nl.tudelft.trustchain.ssi.util.decodeImage
import nl.tudelft.trustchain.ssi.util.encodeImage
import nl.tudelft.trustchain.ssi.util.parseHtml
import java.io.BufferedInputStream
import java.io.InputStream
import java.util.Locale

@SuppressLint("ValidFragment")
class DirectAttestationValueDialog(private val attributeName: String, private val idFormat: String) :
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
                    R.string.cancel
                ) { _, _ ->
                    setResult("", true)
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

class AttestationValueDialog(
    private val attributeName: String,
    private val idFormat: String,
    private val requestedValue: String? = null,
    private val callback: (value: String) -> Unit
) :
    DialogFragment() {

    private lateinit var mView: View
    private var image: Bitmap? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PICK_IMAGE) {
            val imageView = mView.findViewById<ImageView>(R.id.picture_view)
            if (data != null) {
                val inputStream: InputStream? =
                    requireContext().contentResolver.openInputStream(data.data!!)
                val bufferedInputStream = BufferedInputStream(inputStream)
                val imageBM = BitmapFactory.decodeStream(bufferedInputStream)
                val resizedBM = Bitmap.createScaledBitmap(imageBM, 100, 100, true)
                imageView.setImageBitmap(imageBM)
                imageView.visibility = View.VISIBLE
                this.image = resizedBM
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = it.layoutInflater
            val view = inflater.inflate(R.layout.attestation_value_dialog, null)
            this.mView = view

            val attrInput = view.findViewById<TextInputEditText>(R.id.value_input)
            when (idFormat) {
                ID_METADATA_RANGE_18PLUS -> attrInput.inputType = InputType.TYPE_CLASS_NUMBER
                ID_METADATA_RANGE_UNDERAGE -> attrInput.inputType = InputType.TYPE_CLASS_NUMBER
                else -> attrInput.inputType = InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE
            }

            val imageView = view.findViewById<ImageView>(R.id.picture_view)
            val selectImageButton = view.findViewById<Button>(R.id.select_image_button)

            selectImageButton.setOnClickListener { _ ->
                val getIntent = Intent(Intent.ACTION_GET_CONTENT)
                getIntent.type = "image/*"

                val pickIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                pickIntent.type = "image/*"

                val chooserIntent = Intent.createChooser(getIntent, "Select Image")
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(pickIntent))
                startActivityForResult(chooserIntent, PICK_IMAGE)
            }

            if (attributeName == ID_PICTURE.toUpperCase(Locale.getDefault())) {
                attrInput.visibility = View.GONE
                if (requestedValue != null) {
                    val image = decodeImage(requestedValue)
                    imageView.visibility = View.VISIBLE
                    imageView.setImageBitmap(image)
                }
            } else {
                requestedValue?.let { value ->
                    attrInput.setText(
                        value,
                        TextView.BufferType.EDITABLE
                    )
                }
            }

            var truncatedValue: String? = null
            if (requestedValue != null) {
                truncatedValue = if (requestedValue.length > 20) {
                    requestedValue.substring(0, 20) + " ..."
                } else {
                    requestedValue
                }
            }

            val valueString = if (truncatedValue != null) {
                " The requested value is <b>$truncatedValue</b>."
            } else ""

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
                .setMessage(parseHtml("An attestation has been requested for <b>$attributeName</b> with format <b>$idFormat</b>.$valueString"))
            // Create the AlertDialog object and return it
            val dialog = builder.create()
            dialog.setOnShowListener {
                val posBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                posBtn.setOnClickListener {
                    val inputValue =
                        if (attributeName == ID_PICTURE.toUpperCase(Locale.getDefault())) {
                            if (this.image != null) {
                                encodeImage(this.image!!)
                            } else {
                                throw RuntimeException("Image was null!")
                            }
                        } else {
                            attrInput.text.toString()
                        }
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
