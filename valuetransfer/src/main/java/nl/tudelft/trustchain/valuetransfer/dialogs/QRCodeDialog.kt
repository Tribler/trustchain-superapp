package nl.tudelft.trustchain.valuetransfer.dialogs

import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import nl.tudelft.trustchain.valuetransfer.util.createBitmap
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.util.getColorIDFromThemeAttribute
import nl.tudelft.trustchain.valuetransfer.util.setNavigationBarColor
import java.lang.IllegalStateException

class QRCodeDialog(
    private val title: String?,
    private val subtitle: String?,
    private val data: String
) : DialogFragment() {
    private lateinit var parentActivity: ValueTransferMainActivity

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog =
                BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_qrcode, null)

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.behavior.skipCollapsed = true
            bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

            parentActivity = requireActivity() as ValueTransferMainActivity

            setNavigationBarColor(requireContext(), parentActivity, bottomSheetDialog)

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
                    ivQRCode.setImageBitmap(
                        createBitmap(
                            requireContext(),
                            data,
                            R.color.black,
                            getColorIDFromThemeAttribute(parentActivity, R.attr.colorPrimary)
                        )
                    )
                },
                100
            )

            bottomSheetDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
