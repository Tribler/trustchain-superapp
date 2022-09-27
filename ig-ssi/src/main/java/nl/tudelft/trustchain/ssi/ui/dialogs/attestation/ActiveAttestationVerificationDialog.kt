package nl.tudelft.trustchain.ssi.ui.dialogs.attestation

import SuccessDialog
import android.app.Dialog
import android.content.Context
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import nl.tudelft.ipv8.Peer
import nl.tudelft.trustchain.ssi.R
import nl.tudelft.trustchain.ssi.ui.dialogs.status.DangerDialog

class ActiveAttestationVerificationDialog :
    DialogFragment() {
    private lateinit var mView: View
    private lateinit var mContext: Context
    private lateinit var peer: Peer

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater

            val view = inflater.inflate(R.layout.active_verification_dialog, null)
            this.mView = view
            this.mContext = requireContext()

            builder.setView(view)
                .setNegativeButton(
                    R.string.cancel
                ) { _, _ -> }
                .setTitle("Locating Client")
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    fun challengePeer(peer: Peer) {
        Handler(Looper.getMainLooper()).post {
            this.peer = peer
            mView.findViewById<TextView>(R.id.loadingInformation).text =
                getString(R.string.challenging_client)
            val progressBar = mView.findViewById<ProgressBar>(R.id.progressBar)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                progressBar.indeterminateDrawable.colorFilter =
                    BlendModeColorFilter(Color.BLUE, BlendMode.SRC_IN)
            } else {
                @Suppress("DEPRECATION")
                progressBar.indeterminateDrawable
                    .setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_IN)
            }

            this.dialog!!.setTitle("Sending Challenges")
        }
    }

    fun setResult(result: Boolean) {
        Handler(Looper.getMainLooper()).post {
            this.dismiss()
            if (result) {
                SuccessDialog().show(
                    parentFragmentManager,
                    "ig-ssi"
                )
            } else {
                DangerDialog().show(parentFragmentManager, "ig-ssi")
            }
        }
    }

    fun cancel(cancellationMessage: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(requireContext(), cancellationMessage, Toast.LENGTH_LONG).show()
            this.dismiss()
        }
    }
}
