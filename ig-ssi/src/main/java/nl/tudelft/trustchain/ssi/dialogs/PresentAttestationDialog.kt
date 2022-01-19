package nl.tudelft.trustchain.ssi.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Bitmap
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import nl.tudelft.trustchain.ssi.R

class PresentAttestationDialog(
    private val attributeName: String,
    private val attributeValue: String
) :
    DialogFragment() {

    private var mView: View? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater

            @SuppressLint("InflateParams")
            val view = inflater.inflate(R.layout.present_attestation_dialog, null)!!
            mView = view
            builder.setView(mView)

            val dialog: Dialog
            @Suppress("DEPRECATION")
            val title = "Attestation for <font color='#EE0000'>${attributeName.capitalize()}</font>"
            val message = "<b>$attributeName:</b> $attributeValue"
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                builder.setTitle(Html.fromHtml(title, Html.FROM_HTML_MODE_LEGACY))
                dialog = builder.create()
                dialog.setMessage(
                    Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY)
                )
            } else {
                @Suppress("DEPRECATION")
                builder.setTitle(Html.fromHtml(title))
                dialog = builder.create()
                @Suppress("DEPRECATION")
                dialog.setMessage(
                    Html.fromHtml(message)
                )
            }
            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    fun setQRCode(bitmap: Bitmap) {
        val progressBar = mView!!.findViewById<ProgressBar>(R.id.progressBar)
        if (progressBar.isVisible) {
            progressBar.visibility = View.GONE
        }
        mView!!.findViewById<ImageView>(R.id.qrCodeView).setImageBitmap(bitmap)
    }

    fun showError() {
        val textView = mView!!.findViewById<TextView>(R.id.unsupportedAttestation)
        val progressBar = mView!!.findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.GONE
        textView.visibility = View.VISIBLE
    }
}
