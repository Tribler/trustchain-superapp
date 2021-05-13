package nl.tudelft.trustchain.ssi.ui.dialogs.attestation

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.createScaledBitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.Peer
import nl.tudelft.trustchain.ssi.Communication
import nl.tudelft.trustchain.ssi.R
import nl.tudelft.trustchain.ssi.ui.verifier.VerificationFragmentDirections
import nl.tudelft.trustchain.ssi.util.encodeImage
import java.io.BufferedInputStream
import java.io.InputStream
import java.util.Locale

const val AGE = "Name"
const val NAME = "Age"
const val ID_PICTURE = "ID Picture"
const val CUSTOM = "Custom"
val DEFAULT_ATTRIBUTE_NAMES = arrayOf(AGE, NAME, ID_PICTURE, CUSTOM)

const val PICK_IMAGE = 1

@SuppressLint("ClickableViewAccessibility")
class FireMissilesDialog(peer: Peer? = null) :
    DialogFragment() {
    private lateinit var mView: View
    private lateinit var mContext: Context
    private lateinit var peer: Peer
    private var image: Bitmap? = null

    init {
        if (peer != null) {
            this.peer = peer
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PICK_IMAGE) {
            val imageView = mView.findViewById<ImageView>(R.id.id_picture_view)
            if (data != null) {
                val inputStream: InputStream? =
                    requireContext().contentResolver.openInputStream(data.data!!)
                val bufferedInputStream = BufferedInputStream(inputStream)
                val imageBM = BitmapFactory.decodeStream(bufferedInputStream)
                val resizedBM = (createScaledBitmap(imageBM, 10, 10, false))
                imageView.setImageBitmap(resizedBM)
                imageView.visibility = View.VISIBLE
                this.image = resizedBM
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val channel =
                Communication.load()

            val view = inflater.inflate(R.layout.request_attestation_dialog, null)
            this.mView = view
            this.mContext = requireContext()

            val contentView = view.findViewById<LinearLayout>(R.id.content)
            val loaderView = view.findViewById<ConstraintLayout>(R.id.findPeerContent)
            if (this::peer.isInitialized) {
                contentView.visibility = View.VISIBLE
                loaderView.visibility = View.GONE
            }

            val attributeNameSpinner = view.findViewById<Spinner>(R.id.attributeNameSpinner)
            val attributeNameAdapter = ArrayAdapter(
                requireContext(),
                R.layout.support_simple_spinner_dropdown_item,
                DEFAULT_ATTRIBUTE_NAMES
            )

            val attributeNameInput = view.findViewById<TextInputEditText>(R.id.name_input)
            val selectImageButton = view.findViewById<Button>(R.id.select_image_button)
            val imageView = mView.findViewById<ImageView>(R.id.id_picture_view)
            val proposedValueInput = view.findViewById<TextInputEditText>(R.id.value_input)

            attributeNameSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        attributeNameInput.visibility = View.GONE
                    }

                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        when (attributeNameSpinner.selectedItem) {
                            CUSTOM -> attributeNameInput.visibility =
                                View.VISIBLE
                            ID_PICTURE -> {
                                proposedValueInput.visibility = View.GONE
                            }
                            else -> {
                                proposedValueInput.visibility = View.VISIBLE
                                attributeNameInput.visibility =
                                    View.GONE
                                selectImageButton.visibility = View.GONE
                                imageView.visibility = View.GONE
                            }
                        }
                    }
                }

            attributeNameAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
            attributeNameSpinner.adapter = attributeNameAdapter
            attributeNameSpinner.setSelection(0, true)

            val idFormatSpinner = view.findViewById<Spinner>(R.id.idFormatSpinner)
            val idFormatAdapter = ArrayAdapter(
                requireContext(),
                R.layout.support_simple_spinner_dropdown_item,
                channel.attestationOverlay.schemaManager.getSchemaNames().sorted()
            )
            idFormatAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)

            idFormatSpinner.adapter = idFormatAdapter
            idFormatSpinner.setSelection(0, true)

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


            builder.setView(view)
                .setPositiveButton(
                    R.string.fire,
                    null
                )
                .setNegativeButton(
                    R.string.cancel
                ) { _, _ ->
                    findNavController().navigateUp()
                }
                .setTitle("Request Attestation")
            // Create the AlertDialog object and return it
            val dialog = builder.create()
            dialog.setOnShowListener {
                val posBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                if (!this::peer.isInitialized) {
                    posBtn.visibility = View.GONE
                }
                posBtn.setOnClickListener {
                    if (attributeNameSpinner.selectedItem != CUSTOM || attributeNameInput.text.toString() != "") {
                        val idFormat = idFormatSpinner.selectedItem.toString()
                        // val myPeer = Communication.load().myPeer
                        // val key = when (idFormat) {
                        //     ID_METADATA_BIG -> myPeer.identityPrivateKeyBig
                        //     ID_METADATA_HUGE -> myPeer.identityPrivateKeyHuge
                        //     else -> myPeer.identityPrivateKeySmall
                        // }
                        // if (key == null) {
                        //     Log.e("ig-ssi", "Key was null on attestation request.")
                        //     dialog.dismiss()
                        //     AlertDialog.Builder(requireContext())
                        //         .setTitle("Oops!")
                        //         .setMessage("The private keys are not fully initialized yet, try again in a few seconds.") // Specifying a listener allows you to take an action before dismissing the dialog.
                        //         .setPositiveButton(
                        //             "Ok"
                        //         ) { _, _ -> }
                        //         .setIcon(android.R.drawable.ic_dialog_alert)
                        //         .show()
                        // } else {

                        var attributeName =
                            if (attributeNameSpinner.selectedItem != CUSTOM)
                                attributeNameSpinner.selectedItem as String
                            else
                                attributeNameInput.text.toString()
                        attributeName = attributeName.toUpperCase(Locale.getDefault())

                        val proposedValue =
                            if (attributeNameSpinner.selectedItem != ID_PICTURE)
                                proposedValueInput.text.toString()
                            else
                                this.image?.let { it1 -> encodeImage(it1) }


                        Log.d(
                            "ig-ssi",
                            "Sending attestation for ${attributeNameInput.text} to ${peer.mid}"
                        )
                        GlobalScope.launch {
                            channel.requestAttestation(
                                peer,
                                attributeName,
                                idFormat,
                                hashMapOf(),
                                proposedValue
                            )
                        }
                        dialog.dismiss()
                        Toast.makeText(
                            requireContext(),
                            "Requested attestation for ${attributeNameInput.text} from ${peer.mid}",
                            Toast.LENGTH_LONG
                        ).show()
                        // }
                        findNavController().navigateUp()
                    } else {
                        attributeNameInput.error = "Please enter a claim name."
                    }
                }
            }
            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    fun setPeer(peer: Peer) {
        Handler(Looper.getMainLooper()).post {
            this.peer = peer
            mView.findViewById<ConstraintLayout>(R.id.findPeerContent).visibility = View.GONE
            mView.findViewById<LinearLayout>(R.id.content).visibility = View.VISIBLE
            (this.dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).visibility =
                View.VISIBLE
        }
    }

    fun cancel() {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(requireContext(), "Failed to locate peer", Toast.LENGTH_LONG).show()
            this.dismiss()
        }
        findNavController().navigate(R.id.databaseFragment)
    }
}
