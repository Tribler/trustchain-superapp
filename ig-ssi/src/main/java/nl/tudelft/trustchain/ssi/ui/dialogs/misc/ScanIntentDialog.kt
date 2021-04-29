package nl.tudelft.trustchain.ssi.dialogs.misc

import nl.tudelft.trustchain.ssi.dialogs.authority.AuthorityConfirmationDialog
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.Toast
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
import nl.tudelft.trustchain.ssi.dialogs.attestation.FireMissilesDialog
import nl.tudelft.trustchain.ssi.verifier.VerificationFragmentDirections

class ScanIntentDialog(
    private val authorityKey: PublicKey,
    private val rendezvousToken: String?,
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Select Action")
                .setItems(
                    arrayOf("Request attestation", "Register public key as authority"),
                    DialogInterface.OnClickListener { _, which ->
                        when (which) {
                            0 -> {
                                GlobalScope.launch {
                                    val channel = Communication.load(rendezvous = rendezvousToken)
                                    var peer: Peer? = null
                                    try {
                                        withTimeout(30_000) {
                                            while (peer == null) {
                                                peer =
                                                    channel.peers.find { it.publicKey == authorityKey }
                                                delay(50L)
                                            }
                                        }
                                    } catch (e: TimeoutCancellationException) {
                                        Toast.makeText(
                                            requireContext(),
                                            "Could not locate peer",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    FireMissilesDialog(
                                        peer!!
                                    ).show(parentFragmentManager, tag)
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
                )
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
