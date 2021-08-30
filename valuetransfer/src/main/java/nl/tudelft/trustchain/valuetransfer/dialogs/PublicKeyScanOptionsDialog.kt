package nl.tudelft.trustchain.valuetransfer.dialogs

import android.os.Bundle
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import org.json.JSONObject

class PublicKeyScanOptionsDialog(
    private val data: JSONObject,
) : DialogFragment() {

    private lateinit var parentActivity: ValueTransferMainActivity

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {

            val dialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_publickey_scan_options, null)

            parentActivity = requireActivity() as ValueTransferMainActivity

            val publicKey = data.optString("public_key")

            dialog.setContentView(view)
            dialog.show()

            view.findViewById<Button>(R.id.btnAddContact).setOnClickListener {
                dialog.dismiss()
                parentActivity.getQRScanController().addContact(data)
            }

            view.findViewById<Button>(R.id.btnAddAuthority).setOnClickListener {
                dialog.dismiss()
                parentActivity.getQRScanController().addAuthority(publicKey)
            }

            view.findViewById<Button>(R.id.btnAddAttestation).setOnClickListener {
                dialog.dismiss()
                parentActivity.getQRScanController().addAttestation(publicKey)
            }

            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
