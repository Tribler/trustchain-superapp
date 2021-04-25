package nl.tudelft.trustchain.ssi.dialogs.attestation

import SuccessDialog
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.ssi.Communication
import nl.tudelft.trustchain.ssi.dialogs.status.DangerDialog

class AttestationConfirmationDialog(
    private val challenge: ByteArray,
    private val timestamp: Long,
    private val subjectKey: PublicKey,
    private val attestationHash: ByteArray,
    private val metadata: String,
    private val signature: ByteArray,
    private val authorityKeyHash: ByteArray
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Attestation Found")
                .setMessage("Attestation presented by: ${subjectKey.keyToHash().toHex()}")
                .setPositiveButton(
                    "Verify",
                    DialogInterface.OnClickListener { _, _ ->
                        val channel = Communication.load()

                        val success = channel.verifyLocally(
                            challenge,
                            timestamp,
                            subjectKey,
                            attestationHash,
                            metadata,
                            signature,
                            authorityKeyHash
                        )
                        if (success) {
                            SuccessDialog().show(parentFragmentManager, this.tag)
                        } else {
                            DangerDialog().show(parentFragmentManager, this.tag)
                        }
                    }
                )
                .setNegativeButton(
                    "dismiss",
                    DialogInterface.OnClickListener { _, _ ->
                        DangerDialog().show(parentFragmentManager, this.tag)
                    }
                )
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
