package nl.tudelft.trustchain.valuetransfer.dialogs

import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialog
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.databinding.DialogConfirmBinding
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import nl.tudelft.trustchain.valuetransfer.util.setNavigationBarColor

class ConfirmDialog(
    private val title: String,
    private val callback: ((BottomSheetDialog) -> Unit)
) : VTDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog =
                BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val binding = DialogConfirmBinding.inflate(layoutInflater)
            val view = binding.root

            setNavigationBarColor(requireContext(), parentActivity, bottomSheetDialog)

            val tvTitle = binding.tvTitle
            val buttonPositive = binding.btnPositive
            val buttonNegative = binding.btnNegative

            tvTitle.text = title

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            buttonPositive.setOnClickListener {
                callback(bottomSheetDialog)
            }

            buttonNegative.setOnClickListener {
                bottomSheetDialog.dismiss()
            }

            bottomSheetDialog
        }
            ?: throw IllegalStateException(resources.getString(R.string.text_activity_not_null_requirement))
    }
}
