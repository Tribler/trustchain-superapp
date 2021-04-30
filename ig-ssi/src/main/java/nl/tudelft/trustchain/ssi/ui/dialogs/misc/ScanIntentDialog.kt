package nl.tudelft.trustchain.ssi.ui.dialogs.misc

import nl.tudelft.trustchain.ssi.ui.dialogs.authority.AuthorityConfirmationDialog
import android.app.AlertDialog
import android.app.Dialog
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
import nl.tudelft.trustchain.ssi.ui.dialogs.attestation.FireMissilesDialog
import nl.tudelft.trustchain.ssi.ui.verifier.VerificationFragmentDirections

class ScanIntentDialog(
    private val authorityKey: PublicKey,
    private val rendezvousToken: String?,
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { it ->
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Select Action")
                .setItems(
                    arrayOf("Request attestation", "Register public key as authority")
                ) { _, which ->
                    when (which) {
                        0 -> {
                            val dialog = FireMissilesDialog()
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
                                            println("PEERS: ${channel.peers.size}")
                                            delay(100)
                                        }
                                    }
                                    dialog.setPeer(peer!!)
                                } catch (e: TimeoutCancellationException) {
                                    dialog.cancel()
                                }
                            }
                            findNavController().navigate(VerificationFragmentDirections.actionVerificationFragmentToDatabaseFragment())
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
