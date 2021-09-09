package nl.tudelft.trustchain.valuetransfer.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.schema.ID_METADATA_BIG
import nl.tudelft.ipv8.attestation.schema.ID_METADATA_HUGE
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity
import nl.tudelft.ipv8.attestation.wallet.cryptography.bonehexact.BonehPrivateKey
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.util.toggleButton
import java.lang.IllegalStateException

class IdentityAttestationRequestDialog(
    private val peer: Peer,
) : DialogFragment() {

    private lateinit var parentActivity: ValueTransferMainActivity
    private lateinit var trustChainCommunity: TrustChainCommunity
    private lateinit var attestationCommunity: AttestationCommunity

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_identity_attestation_request, null)

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.behavior.skipCollapsed = true
            bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

            parentActivity = requireActivity() as ValueTransferMainActivity
            trustChainCommunity = parentActivity.getCommunity()!!
            attestationCommunity = parentActivity.getCommunity()!!

            val attributeTypeSpinner = view.findViewById<Spinner>(R.id.spinnerAttributeType)
            val attributeNameView = view.findViewById<EditText>(R.id.etAttributeNameValue)

            val requestButton = view.findViewById<Button>(R.id.btnRequestAttestation)
            toggleButton(requestButton, attributeNameView.text.toString().isNotEmpty())

            val attributeNameAdapter = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, attestationCommunity.schemaManager.getSchemaNames().sorted()) {
                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val textView: TextView = super.getDropDownView(position, convertView, parent) as TextView
                    val params = textView.layoutParams
                    params.height = resources.getDimensionPixelSize(R.dimen.textViewHeight)
                    textView.layoutParams = params
                    textView.gravity = Gravity.CENTER_VERTICAL

                    if (position == attributeTypeSpinner.selectedItemPosition) {
                        textView.background = ColorDrawable(Color.LTGRAY)
                    }

                    return textView
                }
            }

            attributeTypeSpinner.adapter = attributeNameAdapter
            attributeTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {}

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    toggleButton(requestButton, attributeNameView.text.isNotEmpty())
                }
            }

            attributeNameView.doAfterTextChanged { state ->
                toggleButton(requestButton, state != null && state.isNotEmpty())
            }

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            requestButton.setOnClickListener {
                attributeNameView.text.toString().let { attributeName ->
                    val idFormat = attributeTypeSpinner.selectedItem.toString()
                    val myPeer = trustChainCommunity.myPeer

                    var privateKey: BonehPrivateKey? = null

                    try {
                        privateKey = when (idFormat) {
                            ID_METADATA_BIG -> myPeer.identityPrivateKeyBig!!
                            ID_METADATA_HUGE -> myPeer.identityPrivateKeyHuge!!
                            else -> myPeer.identityPrivateKeySmall!!
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        attestationCommunity.requestAttestation(
                            peer,
                            attributeName,
                            privateKey!!,
                            hashMapOf("id_format" to idFormat),
                            true
                        )
                    }

                    bottomSheetDialog.dismiss()
                }
            }

            bottomSheetDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
