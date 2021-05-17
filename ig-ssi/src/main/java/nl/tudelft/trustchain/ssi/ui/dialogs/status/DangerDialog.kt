package nl.tudelft.trustchain.ssi.ui.dialogs.status

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import nl.tudelft.trustchain.ssi.R
import nl.tudelft.trustchain.ssi.ui.verifier.VerificationFragmentDirections

class DangerDialog : DialogFragment() {

    lateinit var mDialog: Dialog

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        findNavController().navigateUp()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.danger_dialog, container, false)
        view.setOnClickListener {
            mDialog.dismiss()
        }
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        mDialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        return mDialog
    }
}
