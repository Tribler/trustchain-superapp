package nl.tudelft.trustchain.valuetransfer.dialogs

import android.os.Bundle
import android.os.Handler
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import nl.tudelft.trustchain.valuetransfer.util.createBitmap
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import nl.tudelft.trustchain.valuetransfer.util.setNavigationBarColor
import java.lang.IllegalStateException

class QRCodeDialog(
    private val title: String?,
    private val subtitle: String?,
    private val data: String
) : VTDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_qrcode, null)

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.behavior.apply {
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }

            setNavigationBarColor(requireContext(), parentActivity, bottomSheetDialog)

            view.findViewById<TextView>(R.id.tvTitle).apply {
                isVisible = title != null
                text = title
            }
            view.findViewById<TextView>(R.id.tvSubTitle).apply {
                isVisible = subtitle != null
                text = subtitle
            }

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            @Suppress("DEPRECATION")
            Handler().postDelayed(
                {
                    view.findViewById<ProgressBar>(R.id.pbLoadingSpinner).isVisible = false
                    view.findViewById<ImageView>(R.id.ivQRCode).setImageBitmap(
                        createBitmap(
                            requireContext(),
                            data,
                            R.color.black,
                            R.color.light_gray
                        )
                    )
                },
                100
            )

            bottomSheetDialog
        } ?: throw IllegalStateException(resources.getString(R.string.text_activity_not_null_requirement))
    }
}
