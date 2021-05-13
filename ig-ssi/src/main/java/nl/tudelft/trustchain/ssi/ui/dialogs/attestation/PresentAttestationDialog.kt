package nl.tudelft.trustchain.ssi.ui.dialogs.attestation

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.trustchain.ssi.R
import nl.tudelft.trustchain.ssi.util.decodeImage
import nl.tudelft.trustchain.ssi.util.parseHtml
import java.util.Locale

class PresentAttestationDialog(
    private val attributeName: String,
    private val attributeValue: String?
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

            val title =
                "Attestation for <font color='#EE0000'>${attributeName.capitalize(Locale.getDefault())}</font>"
            val parsedAttributeValue =
                if (attributeValue != null && attributeName != ID_PICTURE.toUpperCase(Locale.getDefault())) {
                    if (attributeValue.length > 500) {
                        attributeValue.substring(0, 500) + " ..."
                    } else {
                        attributeValue
                    }
                } else ""

            if (attributeValue != null && attributeName == ID_PICTURE.toUpperCase(Locale.getDefault())) {
                setImageValue(decodeImage(attributeValue))
            }

            val message = "<b>$attributeName:</b> $parsedAttributeValue"
            builder.setTitle(parseHtml(title))
            val dialog = builder.create()
            dialog.setMessage(parseHtml(message))
            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    fun startTimeout(duration: Long) {
        Handler(Looper.getMainLooper()).post {
            val animatorScale = Settings.Global.getFloat(
                requireContext().contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            val progressBar = mView!!.findViewById<ProgressBar>(R.id.timeoutProgressBar)
            progressBar.visibility = View.VISIBLE
            lifecycleScope.launch {
                ObjectAnimator.ofInt(
                    progressBar,
                    "progress",
                    100, 0
                )
                    .setDuration(duration * (1 / animatorScale).toLong())
                    .start()
                while (isActive && progressBar.progress >= 0) {
                    progressBar.progressDrawable.colorFilter =
                        translateValueToColor(progressBar.progress)
                    delay(100)
                }
            }
        }
    }

    fun setQRCodes(mainQRCode: Bitmap, secondaryQRCode: Bitmap? = null) {
        Handler(Looper.getMainLooper()).post {
            val progressBar = mView!!.findViewById<ProgressBar>(R.id.progressBar)
            if (progressBar.isVisible) {
                progressBar.visibility = View.GONE
            }
            val imageView = mView!!.findViewById<ImageView>(R.id.qrCodeView)

            if (secondaryQRCode != null) {
                val switch = mView!!.findViewById<Switch>(R.id.QRSwitch)
                if (switch.isChecked) {
                    imageView.setImageBitmap(secondaryQRCode)
                } else {
                    imageView.setImageBitmap(mainQRCode)
                }
                switch.visibility = View.VISIBLE
                switch.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        imageView.setImageBitmap(secondaryQRCode)
                    } else {
                        imageView.setImageBitmap(mainQRCode)
                    }
                }
            } else {
                imageView.setImageBitmap(mainQRCode)
            }
        }
    }

    fun showError() {
        val textView = mView!!.findViewById<TextView>(R.id.unsupportedAttestation)
        val progressBar = mView!!.findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.GONE
        textView.visibility = View.VISIBLE
    }

    private fun translateValueToColor(value: Int): PorterDuffColorFilter {
        val r = (255 * (100 - value)) / 100
        val g = (255 * value) / 100
        val b = 0
        val color = android.graphics.Color.argb(255, r, g, b)
        return PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY)
    }

    private fun setImageValue(image: Bitmap) {
        val imageView = this.mView!!.findViewById<ImageView>(R.id.pictureValueView)
        imageView.setImageBitmap(image)
        imageView.visibility = View.VISIBLE
    }
}
