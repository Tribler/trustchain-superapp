package nl.tudelft.trustchain.ssi.ui.dialogs.attestation

import SuccessDialog
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.identity.Metadata
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.util.stripSHA1Padding
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.ipv8.util.toKey
import nl.tudelft.trustchain.ssi.Communication
import nl.tudelft.trustchain.ssi.ui.dialogs.status.DangerDialog

const val VERIFICATION_THRESHOLD = 0.9

class AttestationConfirmationDialog(
    private val attestationHash: ByteArray,
    private val attestationName: String,
    private val attestationValue: ByteArray?,
    private val idFormat: String,
    private val metadata: Metadata,
    private val subjectKey: PublicKey,
    private val challengePair: Pair<ByteArray, Long>,
    private val attestors: List<Pair<ByteArray, ByteArray>>,
    private val rendezvousToken: String
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Attestation Found")
                .setMessage("Attestation presented by: ${subjectKey.keyToHash().toHex()}")
                .setPositiveButton(
                    "Quick Verification"
                ) { _, _ ->
                    val channel = Communication.load()

                    val success = attestationValue?.let { it1 ->
                        channel.verifyLocally(
                            attestationHash,
                            it1,
                            metadata,
                            subjectKey,
                            challengePair,
                            attestors
                        )
                    } == true
                    if (success) {
                        SuccessDialog().show(parentFragmentManager, this.tag)
                    } else {
                        DangerDialog().show(parentFragmentManager, this.tag)
                    }
                }
                .setNegativeButton(
                    "Active Verification"
                ) { _, _ ->
                    val dialog = ActiveVerificationDialog()
                    dialog.show(parentFragmentManager, "ig-ssi")
                    GlobalScope.launch {
                        val channel =
                            Communication.load(rendezvous = rendezvousToken)
                        var peer: Peer? = null
                        try {
                            withTimeout(30_000) {
                                while (peer == null) {
                                    peer =
                                        channel.peers.find { it1 ->
                                            it1.publicKey.keyToHash()
                                                .contentEquals(subjectKey.keyToHash())
                                        }
                                    delay(100)
                                }
                            }
                            dialog.challengePeer(peer!!)
                        } catch (e: TimeoutCancellationException) {
                            dialog.cancel("Failed to locate peer.")
                        }

                        var success: Boolean? = null
                        try {
                            withTimeout(60_000) {
                                // TODO: Add dialog for attestation value.
                                val hash = stripSHA1Padding(attestationHash)
                                channel.verify(peer!!, hash, listOf(attestationValue!!), idFormat)
                                while (success == null) {
                                    if (channel.verificationOutput[hash.toKey()]!!.all { output -> output.second != null }) {
                                        success =
                                            channel.verificationOutput[hash.toKey()]!!.all { output -> output.second!! > VERIFICATION_THRESHOLD }
                                    }
                                    delay(100)
                                }
                            }
                            dialog.setResult(success!!)
                        } catch (e: TimeoutCancellationException) {
                            dialog.cancel("Peer failed to respond to verification request in time.")
                        }
                    }
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
