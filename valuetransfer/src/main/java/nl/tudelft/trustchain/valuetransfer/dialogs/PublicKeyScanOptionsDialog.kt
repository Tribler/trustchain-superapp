package nl.tudelft.trustchain.valuetransfer.dialogs

import android.os.Bundle
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.util.setNavigationBarColor
import org.json.JSONObject

class PublicKeyScanOptionsDialog(
    private val data: JSONObject,
) : DialogFragment() {

    private lateinit var parentActivity: ValueTransferMainActivity

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {

            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_publickey_scan_options, null)

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.behavior.skipCollapsed = true
            bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

            parentActivity = requireActivity() as ValueTransferMainActivity

            setNavigationBarColor(requireContext(), parentActivity, bottomSheetDialog)

            val publicKey = data.optString("public_key")

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            view.findViewById<Button>(R.id.btnAddContact).setOnClickListener {
                bottomSheetDialog.dismiss()
                parentActivity.getQRScanController().addContact(data)
            }

            view.findViewById<Button>(R.id.btnAddAuthority).setOnClickListener {
                bottomSheetDialog.dismiss()
                parentActivity.getQRScanController().addAuthority(publicKey)
            }

            view.findViewById<Button>(R.id.btnAddAttestation).setOnClickListener {
                bottomSheetDialog.dismiss()
                parentActivity.getQRScanController().addAttestation(publicKey)
            }

            bottomSheetDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
