package nl.tudelft.trustchain.valuetransfer.ui

import android.app.Dialog
import android.os.Bundle
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.util.setNavigationBarColor

abstract class VTBottomSheetDialogFragment(
    private val contentLayoutId: Int = 0,
    private val isDraggable: Boolean = true
) : VTDialogFragment() {

    lateinit var bottomSheetDialog: BottomSheetDialog
    lateinit var dialogView: View

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
        dialogView = layoutInflater.inflate(contentLayoutId, null)

        setNavigationBarColor(requireContext(), parentActivity, bottomSheetDialog)

        // Dialog cannot be discarded on outside touch
        bottomSheetDialog.setCancelable(false)
        bottomSheetDialog.setCanceledOnTouchOutside(false)

        // Fix keyboard exposing over content of dialog
        bottomSheetDialog.behavior.apply {
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }

        // Force the dialog to be undraggable
        if (!isDraggable) {
            bottomSheetDialog.behavior.addBottomSheetCallback(
                object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                            bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
                        }
                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) {}
                }
            )
        }

        return activity?.let {
            bottomSheetDialog.setContentView(dialogView)
            bottomSheetDialog.show()

            bottomSheetDialog
        } ?: throw IllegalStateException(resources.getString(R.string.text_activity_not_null_requirement))
    }
}
