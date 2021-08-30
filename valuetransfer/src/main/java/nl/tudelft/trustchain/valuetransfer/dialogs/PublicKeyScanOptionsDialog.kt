package nl.tudelft.trustchain.valuetransfer.dialogs

import android.os.Bundle
import android.util.Log
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

            val buttonAddContact = view.findViewById<Button>(R.id.btnAddContact)
            val buttonAddAuthority = view.findViewById<Button>(R.id.btnAddAuthority)
            val buttonAddAttestation = view.findViewById<Button>(R.id.btnAddAttestation)

            val publicKey = data.optString("public_key")

            dialog.setContentView(view)
            dialog.show()

            buttonAddContact.setOnClickListener {
                dialog.dismiss()
                Log.d("VTLOG", "ADD CONTACT")
                Log.d("VTLOG", publicKey)

                parentActivity.getQRScanController().addContact(data)
            }

            buttonAddAuthority.setOnClickListener {
                dialog.dismiss()
                Log.d("VTLOG", "ADD AUTHORITY")
                Log.d("VTLOG", publicKey)

                parentActivity.getQRScanController().addAuthority(publicKey)
            }

            buttonAddAttestation.setOnClickListener {
                dialog.dismiss()
                Log.d("VTLOG", "ADD ATTESTATION")
                Log.d("VTLOG", publicKey)

                parentActivity.getQRScanController().addAttestation(publicKey)
            }

            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
