package nl.tudelft.trustchain.ssi.ui.dialogs.authority

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.ssi.Communication
import nl.tudelft.trustchain.ssi.R
import nl.tudelft.trustchain.ssi.ui.verifier.VerificationFragmentDirections

class AuthorityConfirmationDialog(
    private val authorityKey: PublicKey,
    private val navigateUp: Boolean = true
) : DialogFragment() {

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (navigateUp) {
            findNavController().navigateUp()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Authority Found")
                .setMessage("Address: ${authorityKey.keyToHash().toHex()}")
                .setPositiveButton(
                    "Add"
                ) { _, _ ->
                    val authorityManager =
                        Communication.load().authorityManager
                    if (!authorityManager.containsAuthority(authorityKey.keyToHash())) {
                        authorityManager.addTrustedAuthority(
                            authorityKey
                        )

                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(
                                context,
                                "Successfully acknowledged Authority.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(
                                context,
                                "Authority already acknowledged.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                .setNegativeButton(
                    R.string.cancel
                ) { _, _ ->
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            context,
                            "Cancelled acknowledging Authority.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
