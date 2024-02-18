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
import nl.tudelft.trustchain.valuetransfer.databinding.DialogQrcodeBinding
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
            val bottomSheetDialog =
                BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val binding = DialogQrcodeBinding.inflate(it.layoutInflater)
            val view = binding.root

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.behavior.apply {
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }

            setNavigationBarColor(requireContext(), parentActivity, bottomSheetDialog)

            binding.tvTitle.apply {
                isVisible = title != null
                text = title
            }

            binding.tvSubTitle.apply {
                isVisible = subtitle != null
                text = subtitle
            }

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            @Suppress("DEPRECATION")
            Handler().postDelayed(
                {
                    binding.pbLoadingSpinner.isVisible = false
                    binding.ivQRCode.setImageBitmap(
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
        }
            ?: throw IllegalStateException(resources.getString(R.string.text_activity_not_null_requirement))
    }
}
