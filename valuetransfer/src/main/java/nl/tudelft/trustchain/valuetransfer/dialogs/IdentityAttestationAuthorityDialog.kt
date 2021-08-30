package nl.tudelft.trustchain.valuetransfer.dialogs

import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity
import nl.tudelft.ipv8.attestation.wallet.cryptography.attest
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import org.json.JSONObject
import java.lang.IllegalStateException

class IdentityAttestationAuthorityDialog(
    private val authorityKey: PublicKey,
) : DialogFragment() {

    private lateinit var parentActivity: ValueTransferMainActivity
    private lateinit var attestationCommunity: AttestationCommunity

    private lateinit var dialogView: View

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_identity_attestation_authority, null)

            parentActivity = requireActivity() as ValueTransferMainActivity
            attestationCommunity = parentActivity.getCommunity(ValueTransferMainActivity.attestationCommunityTag) as AttestationCommunity
            dialogView = view

            val authorityAddressValue = view.findViewById<EditText>(R.id.etAuthorityAddressValue)
            val addAuthorityButton = view.findViewById<Button>(R.id.btnAddAuthority)

            authorityAddressValue.setText(authorityKey.keyToBin().toHex())

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            addAuthorityButton.setOnClickListener {
                val authorityManager = attestationCommunity.trustedAuthorityManager
                when(!authorityManager.contains(authorityKey.keyToHash().toHex())) {
                    true -> {
                        authorityManager.addTrustedAuthority(authorityKey)
                        parentActivity.displaySnackbar(requireContext(), "Authority has been added")
//                        Toast.makeText(requireContext(), "Authority has been added", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        parentActivity.displaySnackbar(requireContext(), "Authority already been added before", view = view.rootView, type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR, extraPadding = true)
//                        Toast.makeText(requireContext(), "Authority already been added before", Toast.LENGTH_SHORT).show()
                    }
                }

                bottomSheetDialog.dismiss()
            }

            bottomSheetDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
