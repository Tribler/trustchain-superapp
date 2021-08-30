package nl.tudelft.trustchain.valuetransfer.dialogs

import android.content.Intent
import android.os.Bundle
import android.renderscript.Sampler
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.Authority
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.ssi.peers.AuthorityItem
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.ui.identity.AttestationAuthorityItemRenderer
import nl.tudelft.trustchain.valuetransfer.ui.identity.IdentityFragment
import org.json.JSONObject
import java.lang.IllegalStateException

class IdentityAttestationAuthoritiesDialog(
    private val myPublicKey: String
) : DialogFragment() {

    private lateinit var parentActivity: ValueTransferMainActivity
    private lateinit var attestationCommunity: AttestationCommunity

    private lateinit var dialogView: View

    private val adapterAuthorities = ItemAdapter()

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_identity_attestation_authorities, null)

            parentActivity = requireActivity() as ValueTransferMainActivity
            attestationCommunity = parentActivity.getCommunity(ValueTransferMainActivity.attestationCommunityTag) as AttestationCommunity
            dialogView = view

            val authoritiesRecyclerView = view.findViewById<RecyclerView>(R.id.rvAuthorities)
            val addAuthorityButton = view.findViewById<Button>(R.id.btnAddAuthority)

            adapterAuthorities.registerRenderer(
                AttestationAuthorityItemRenderer(
                    myPublicKey
                ) { authorityItem ->
                    ConfirmDialog("Are u sure to remove this authority?") { dialog ->
                        attestationCommunity.trustedAuthorityManager.deleteTrustedAuthority(
                            authorityItem.publicKeyHash
                        )
                        parentActivity.displaySnackbar(requireContext(), "Authority has been removed")
                        dialog.dismiss()
                    }.show(parentFragmentManager, tag)
                }
            )

            lifecycleScope.launchWhenCreated {
                while(isActive) {
                    loadAuthorities()
                    delay(1000)
                }
            }

            authoritiesRecyclerView.adapter = adapterAuthorities
            authoritiesRecyclerView.layoutManager = LinearLayoutManager(requireContext())

            addAuthorityButton.setOnClickListener {
                QRCodeUtils(requireContext()).startQRScanner(this, promptText = "Scan public key of signee to add as authority", vertical = true)
            }

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            bottomSheetDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun loadAuthorities() {
        val authorities = createAuthoritiesItems(
            attestationCommunity.trustedAuthorityManager.getAuthorities()
        )
        if(adapterAuthorities.itemCount != authorities.size) {
            adapterAuthorities.updateItems(authorities)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        QRCodeUtils(requireContext()).parseActivityResult(requestCode, resultCode, data)?.let { result ->
            try {
                val obj = JSONObject(result)

                if(obj.has("public_key")) {
                    try {
                        defaultCryptoProvider.keyFromPublicBin(obj.optString("public_key").hexToBytes())
                        val publicKey = obj.optString("public_key")

                        parentActivity.getQRScanController().addAuthority(publicKey)
                    }catch(e: Exception) {
                        e.printStackTrace()
                        parentActivity.displaySnackbar(requireContext(), "Invalid public key in QR-code", view = dialogView.rootView, type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR, extraPadding = true)
//                        Toast.makeText(requireContext(), "Invalid public key in QR-code", Toast.LENGTH_SHORT).show()
                    }
                }else{
                    parentActivity.displaySnackbar(requireContext(), "No public key found in QR-code", view = dialogView.rootView, type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR, extraPadding = true)
//                    Toast.makeText(requireContext(), "No public key found in QR-code", Toast.LENGTH_SHORT).show()
                }
            }catch(e: Exception) {
                e.printStackTrace()
                parentActivity.displaySnackbar(requireContext(), "Scanned QR code not in JSON format", view = dialogView.rootView, type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR, extraPadding = true)
//                Toast.makeText(requireContext(), "Scanned QR code not in JSON format", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createAuthoritiesItems(authorities: List<Authority>): List<AuthorityItem> {
        return authorities.map {
            AuthorityItem(
                it.publicKey,
                it.hash,
                ""
            )
        }
    }
}
