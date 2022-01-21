package nl.tudelft.trustchain.ssi.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputEditText
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.schema.ID_METADATA_BIG
import nl.tudelft.ipv8.attestation.schema.ID_METADATA_HUGE
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity
import nl.tudelft.trustchain.ssi.R
import java.util.*

class FireMissilesDialog(private val peer: Peer) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val attestationCommunity =
                IPv8Android.getInstance().getOverlay<AttestationCommunity>()!!
            val view = inflater.inflate(R.layout.request_attestation_dialog, null)
            val spinner = view.findViewById<Spinner>(R.id.idFormatSpinner)
            val arrayAdapter = ArrayAdapter(
                requireContext(),
                R.layout.support_simple_spinner_dropdown_item,
                attestationCommunity.schemaManager.getSchemaNames().sorted()
            )
            arrayAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
            spinner.adapter = arrayAdapter
            spinner.setSelection(3, true)

            builder.setView(view)
                .setPositiveButton(
                    R.string.fire,
                    null
                )
                .setNegativeButton(
                    R.string.cancel,
                    DialogInterface.OnClickListener { _, _ -> }
                )
                .setTitle("Request Attestation")
            // Create the AlertDialog object and return it
            val dialog = builder.create()
            dialog.setOnShowListener {
                val posBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                posBtn.setOnClickListener {
                    val attrInput = view.findViewById<TextInputEditText>(R.id.attribute_input)
                    if (attrInput.text.toString() != "") {
                        val idFormat = spinner.selectedItem.toString()
                        val myPeer = IPv8Android.getInstance().myPeer
                        val key = when (idFormat) {
                            ID_METADATA_BIG -> myPeer.identityPrivateKeyBig
                            ID_METADATA_HUGE -> myPeer.identityPrivateKeyHuge
                            else -> myPeer.identityPrivateKeySmall
                        }
                        if (key == null) {
                            Log.e("ig-ssi", "Key was null on attestation request.")
                            dialog.dismiss()
                            AlertDialog.Builder(requireContext())
                                .setTitle("Oops!")
                                .setMessage("The private keys are not fully initialized yet, try again in a few seconds.") // Specifying a listener allows you to take an action before dismissing the dialog.
                                .setPositiveButton(
                                    "Ok"
                                ) { _, _ -> }
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show()
                        } else {
                            attestationCommunity.requestAttestation(
                                peer,
                                @Suppress("DEPRECATION")
                                attrInput.text.toString().toUpperCase(Locale.getDefault()),
                                key,
                                hashMapOf("id_format" to idFormat),
                                true
                            )
                            Log.d(
                                "ig-ssi",
                                "Sending attestation for ${attrInput.text} to ${peer.mid}"
                            )
                            dialog.dismiss()
                            Toast.makeText(
                                requireContext(),
                                "Requested attestation for ${attrInput.text} from ${peer.mid}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        attrInput.error = "Please enter a claim name."
                    }
                }
            }
            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
