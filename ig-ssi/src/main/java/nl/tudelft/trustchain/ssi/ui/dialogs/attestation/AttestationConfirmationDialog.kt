package nl.tudelft.trustchain.ssi.dialogs.attestation

import SuccessDialog
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import nl.tudelft.ipv8.attestation.identity.Metadata
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.ssi.Communication
import nl.tudelft.trustchain.ssi.dialogs.status.DangerDialog

class AttestationConfirmationDialog(
    private val attestationHash: ByteArray,
    private val attestationName: String,
    private val attestationValue: ByteArray?,
    private val metadata: Metadata,
    private val subjectKey: PublicKey,
    private val challengePair: Pair<ByteArray, Long>,
    private val attestors: List<Pair<ByteArray, ByteArray>>
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Attestation Found")
                .setMessage("Attestation presented by: ${subjectKey.keyToHash().toHex()}")
                .setPositiveButton(
                    "Verify"
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
                    "dismiss"
                ) { _, _ ->
                    DangerDialog().show(parentFragmentManager, this.tag)
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
