package nl.tudelft.trustchain.ssi.dialogs.attestation

import android.content.DialogInterface
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import nl.tudelft.trustchain.ssi.R
import nl.tudelft.trustchain.ssi.database.DatabaseItem

class RemoveAttestationDialog(val item: DatabaseItem, val callback: (() -> Unit) = {}) :
    DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            builder.setView(view)
                .setPositiveButton(
                    "Delete Attestation",
                    DialogInterface.OnClickListener { _, _ ->
                        // TODO: Implement removal.
                        // IPv8Android.getInstance()
                        //     .getOverlay<AttestationCommunity>()!!.database.deleteAttestationByHash(
                        //         item.attestationBlob.attestationHash
                        //     )
                        Toast.makeText(
                            requireContext(),
                            "Successfully deleted attestation",
                            Toast.LENGTH_LONG
                        ).show()
                        callback()
                    }
                )
                .setNegativeButton(
                    R.string.cancel,
                    DialogInterface.OnClickListener { _, _ ->
                        Toast.makeText(
                            requireContext(),
                            "Cancelled deletion",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
                .setTitle("Delete Attestation permanently?")
                .setIcon(R.drawable.ic_round_warning_amber_24)
                .setMessage("Note: this action cannot be undone.")
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
