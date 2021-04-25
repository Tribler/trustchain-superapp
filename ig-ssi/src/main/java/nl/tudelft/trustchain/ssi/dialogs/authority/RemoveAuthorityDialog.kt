package nl.tudelft.trustchain.ssi.dialogs.authority

import android.content.DialogInterface
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.ssi.Communication
import nl.tudelft.trustchain.ssi.R
import nl.tudelft.trustchain.ssi.peers.AuthorityItem

class RemoveAuthorityDialog(val item: AuthorityItem, val callback: (() -> Unit) = { }) :
    DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            builder.setView(view)
                .setPositiveButton(
                    "Delete Authority",
                    DialogInterface.OnClickListener { _, _ ->
                        Communication.getInstance()
                            .authorityManager.deleteTrustedAuthority(
                                item.publicKeyHash.hexToBytes()
                            )
                        Toast.makeText(
                            requireContext(),
                            "Successfully deleted authority",
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
                        callback()
                    }
                )
                .setTitle("Delete Authority permanently?")
                .setIcon(R.drawable.ic_round_warning_amber_24)
                .setMessage("Note: this action cannot be undone.")
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
