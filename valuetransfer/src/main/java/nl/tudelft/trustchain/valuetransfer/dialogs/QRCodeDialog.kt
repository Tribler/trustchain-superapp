package nl.tudelft.trustchain.valuetransfer.dialogs

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.valuetransfer.R
import org.json.JSONObject
import java.lang.IllegalStateException

class QRCodeDialog(
    private val title: String?,
    private val subtitle: String?,
    private val map: Map<String, String>
) : DialogFragment() {

    private val qrCodeUtils by lazy {
        QRCodeUtils(requireContext())
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog =
                BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_qrcode, null)

            val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
            val tvSubTitle = view.findViewById<TextView>(R.id.tvSubTitle)
            val ivQRCode = view.findViewById<ImageView>(R.id.ivQRCode)

            tvTitle.isVisible = (title != null)
            tvSubTitle.isVisible = (subtitle != null)
            tvTitle.text = title
            tvSubTitle.text = subtitle

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            Handler().postDelayed(
                Runnable {
                    view.findViewById<ProgressBar>(R.id.pbLoadingSpinner).visibility = View.GONE
                    ivQRCode.setImageBitmap(createBitmap(map))
                }, 100
            )

            bottomSheetDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun createBitmap(attributes: Map<String, String>): Bitmap {
        val data = JSONObject()
        for ((key, value) in attributes) {
            data.put(key, value)
        }

        return qrCodeUtils.createQR(data.toString(), pColor = R.color.white, bColor = R.color.colorPrimaryValueTransfer)!!
    }
}
