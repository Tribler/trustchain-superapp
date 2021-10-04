package nl.tudelft.trustchain.valuetransfer.dialogs

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.attestation.Authority
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.ssi.peers.AuthorityItem
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.ui.QRScanController
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import nl.tudelft.trustchain.valuetransfer.ui.identity.AttestationAuthorityItemRenderer
import nl.tudelft.trustchain.valuetransfer.util.setNavigationBarColor
import org.json.JSONObject
import java.lang.IllegalStateException

class IdentityAttestationAuthoritiesDialog(
    private val myPublicKey: String
) : VTDialogFragment() {

    private lateinit var dialogView: View

    private val adapterAuthorities = ItemAdapter()

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_identity_attestation_authorities, null)

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.behavior.apply {
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }

            dialogView = view

            setNavigationBarColor(requireContext(), parentActivity, bottomSheetDialog)

            val authoritiesRecyclerView = view.findViewById<RecyclerView>(R.id.rvAuthorities)
            val addAuthorityButton = view.findViewById<Button>(R.id.btnAddAuthority)

            adapterAuthorities.registerRenderer(
                AttestationAuthorityItemRenderer(
                    myPublicKey
                ) { authorityItem ->
                    ConfirmDialog(
                        resources.getString(
                            R.string.text_confirm_delete,
                            resources.getString(R.string.text_this_authority)
                        )
                    ) { dialog ->
                        getAttestationCommunity().trustedAuthorityManager.deleteTrustedAuthority(
                            authorityItem.publicKeyHash
                        )
                        parentActivity.displaySnackbar(
                            requireContext(),
                            resources.getString(R.string.snackbar_authority_remove_success),
                            view = view.rootView
                        )
                        dialog.dismiss()
                    }.show(parentFragmentManager, tag)
                }
            )

            lifecycleScope.launchWhenCreated {
                while (isActive) {
                    loadAuthorities()
                    delay(1000)
                }
            }

            authoritiesRecyclerView.adapter = adapterAuthorities
            authoritiesRecyclerView.layoutManager = LinearLayoutManager(requireContext())

            addAuthorityButton.setOnClickListener {
                QRCodeUtils(requireContext()).startQRScanner(
                    this,
                    promptText = resources.getString(R.string.text_scan_public_key_to_add_authority),
                    vertical = true
                )
            }

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            bottomSheetDialog
        } ?: throw IllegalStateException(resources.getString(R.string.text_activity_not_null_requirement))
    }

    private fun loadAuthorities() {
        createAuthoritiesItems(
            getAttestationCommunity().trustedAuthorityManager.getAuthorities()
        ).apply {
            if (this.size != adapterAuthorities.itemCount)
                adapterAuthorities.updateItems(this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        QRCodeUtils(requireContext()).parseActivityResult(requestCode, resultCode, data)?.let { result ->
            try {
                val obj = JSONObject(result)

                if (obj.has(QRScanController.KEY_PUBLIC_KEY)) {
                    try {
                        defaultCryptoProvider.keyFromPublicBin(obj.optString(QRScanController.KEY_PUBLIC_KEY).hexToBytes())
                        val publicKey = obj.optString(QRScanController.KEY_PUBLIC_KEY)

                        this.dismiss()
                        parentActivity.getQRScanController().addAuthority(publicKey)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        parentActivity.displaySnackbar(
                            requireContext(),
                            resources.getString(R.string.snackbar_invalid_public_key),
                            view = dialogView.rootView,
                            type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR
                        )
                    }
                } else {
                    parentActivity.displaySnackbar(
                        requireContext(),
                        resources.getString(R.string.snackbar_no_public_key_found),
                        view = dialogView.rootView,
                        type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                parentActivity.displaySnackbar(
                    requireContext(),
                    resources.getString(R.string.snackbar_qr_code_not_json_format),
                    view = dialogView.rootView,
                    type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR
                )
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
