package nl.tudelft.trustchain.valuetransfer.dialogs

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import nl.tudelft.trustchain.valuetransfer.R


class ConfirmDialog(
    private val title: String,
    private val callback: ((BottomSheetDialog) -> Unit)
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {

            val dialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_confirm, null)

            val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
            val buttonPositive = view.findViewById<Button>(R.id.btnPositive)
            val buttonNegative = view.findViewById<Button>(R.id.btnNegative)

            tvTitle.text = title

            dialog.setContentView(view)
            dialog.show()

            buttonPositive.setOnClickListener {
                callback(dialog)
            }

            buttonNegative.setOnClickListener {
                dialog.dismiss()
            }

            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
