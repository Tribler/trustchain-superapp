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

class AuthorityConfirmationDialog(private val authorityKey: PublicKey) : DialogFragment() {

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        findNavController().navigateUp()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Authority Found")
                .setMessage("Address: ${authorityKey.keyToHash().toHex()}")
                .setPositiveButton(
                    "Add",
                    DialogInterface.OnClickListener { _, _ ->
                        val authorityManager =
                            Communication.load().attestationOverlay.authorityManager
                        if (!authorityManager.contains(authorityKey.keyToHash())) {
                            authorityManager.addTrustedAuthority(
                                authorityKey
                            )

                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(
                                    context,
                                    "Successfully added new Authority",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(
                                    context,
                                    "Authority already added",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        findNavController().navigate(VerificationFragmentDirections.actionVerificationFragmentToPeersFragment2())
                    }
                )
                .setNegativeButton(
                    R.string.cancel,
                    DialogInterface.OnClickListener { _, _ ->
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(
                                context,
                                "Cancelled new Authority",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        findNavController().navigate(VerificationFragmentDirections.actionVerificationFragmentToDatabaseFragment())
                    }
                )

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
