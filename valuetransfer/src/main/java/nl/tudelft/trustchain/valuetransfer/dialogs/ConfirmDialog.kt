package nl.tudelft.trustchain.valuetransfer.dialogs

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import nl.tudelft.trustchain.valuetransfer.util.setNavigationBarColor

class ConfirmDialog(
    private val title: String,
    private val callback: ((BottomSheetDialog) -> Unit)
) : VTDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {

            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_confirm, null)

            setNavigationBarColor(requireContext(), parentActivity, bottomSheetDialog)

            val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
            val buttonPositive = view.findViewById<Button>(R.id.btnPositive)
            val buttonNegative = view.findViewById<Button>(R.id.btnNegative)

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
        } ?: throw IllegalStateException(resources.getString(R.string.text_activity_not_null_requirement))
    }
}
