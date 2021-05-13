package nl.tudelft.trustchain.ssi.ui.dialogs.misc

import nl.tudelft.trustchain.ssi.ui.dialogs.authority.AuthorityConfirmationDialog
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.trustchain.ssi.Communication
import nl.tudelft.trustchain.ssi.ui.dialogs.attestation.RequestAttestationDialog

class ScanIntentDialog(
    private val authorityKey: PublicKey,
    private val rendezvousToken: String?,
) : DialogFragment() {

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        findNavController().navigateUp()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { it ->
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Select Action")
                .setItems(
                    arrayOf("Request attestation", "Register public key as authority")
                ) { _, which ->
                    when (which) {
                        0 -> {
                            val dialog = RequestAttestationDialog()
                            dialog.show(parentFragmentManager, "ig-ssi")
                            GlobalScope.launch {
                                val channel =
                                    Communication.load(rendezvous = rendezvousToken)
                                try {
                                    var peer: Peer? = null
                                    withTimeout(30_000) {
                                        while (peer == null) {
                                            peer =
                                                channel.peers.find { it1 ->
                                                    it1.publicKey.keyToHash()
                                                        .contentEquals(authorityKey.keyToHash())
                                                }
                                            delay(100)
                                        }
                                    }
                                    dialog.setPeer(peer!!)
                                } catch (e: TimeoutCancellationException) {
                                    dialog.cancel()
                                }
                            }
                        }
                        1 -> {
                            AuthorityConfirmationDialog(authorityKey).show(
                                parentFragmentManager,
                                this.tag
                            )
                        }
                    }
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
