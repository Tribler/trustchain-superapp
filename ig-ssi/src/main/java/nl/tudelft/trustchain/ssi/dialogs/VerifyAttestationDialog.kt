package nl.tudelft.trustchain.ssi.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputEditText
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.wallet.AttestationBlob
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity
import nl.tudelft.trustchain.ssi.R

class VerifyAttestationDialog(private val databaseBlob: AttestationBlob) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val attestationCommunity =
                IPv8Android.getInstance().getOverlay<AttestationCommunity>()!!
            val view = inflater.inflate(R.layout.verify_attestation_dialog, null)
            builder.setView(view)
                .setPositiveButton(
                    R.string.fire,
                    DialogInterface.OnClickListener { _, _ ->
                        val addressInput =
                            view.findViewById<TextInputEditText>(R.id.peer_address_input).text.toString()
                        val attributeName =
                            view.findViewById<TextInputEditText>(R.id.attribute_name_input).text.toString()
                        val input = addressInput.split(":").toTypedArray()
                        val ip = input[0]
                        val port = input[1].toInt()
                        var address: IPv4Address? = null
                        for (peer in attestationCommunity.getPeers()) {
                            if (peer.address.toString() == addressInput) {
                                address = IPv4Address(ip, port)
                            }
                        }
                        if (address == null) {
                            throw RuntimeException("IPv4 Address not found")
                        }
                        Log.d("ig-ssi", "Sending verify request")
                        attestationCommunity.verifyAttestationValues(
                            address,
                            databaseBlob.attestationHash,
                            arrayListOf(attributeName.toByteArray()),
                            ::verifyComplete,
                            databaseBlob.idFormat
                        )
                    }
                )
                .setNegativeButton(
                    R.string.cancel,
                    DialogInterface.OnClickListener { _, _ -> }
                )
                .setTitle("Request Attestation")
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun verifyComplete(hash: ByteArray, values: List<Double>) {
        Log.d("ig-ssi", "VerifyComplete for hash: $hash, with values:")
        values.forEachIndexed { index, d -> Log.d("ig-ssi", "Value $index: $d") }
    }
}
