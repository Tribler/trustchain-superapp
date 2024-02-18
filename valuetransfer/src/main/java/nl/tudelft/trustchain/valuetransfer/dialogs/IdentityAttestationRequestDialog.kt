package nl.tudelft.trustchain.valuetransfer.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.schema.ID_METADATA_BIG
import nl.tudelft.ipv8.attestation.schema.ID_METADATA_HUGE
import nl.tudelft.ipv8.attestation.wallet.cryptography.bonehexact.BonehPrivateKey
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.databinding.DialogIdentityAttestationRequestBinding
import nl.tudelft.trustchain.valuetransfer.ui.QRScanController
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import nl.tudelft.trustchain.valuetransfer.util.setNavigationBarColor
import nl.tudelft.trustchain.valuetransfer.util.toggleButton
import java.lang.IllegalStateException

class IdentityAttestationRequestDialog(
    private val peer: Peer,
) : VTDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog =
                BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val binding = DialogIdentityAttestationRequestBinding.inflate(layoutInflater)
            val view = binding.root

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.behavior.apply {
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }

            setNavigationBarColor(requireContext(), parentActivity, bottomSheetDialog)

            val attributeTypeSpinner = binding.spinnerAttributeType
            val attributeNameView = binding.etAttributeNameValue

            val requestButton = binding.btnRequestAttestation
            toggleButton(requestButton, attributeNameView.text.toString().isNotEmpty())

            val attributeNameAdapter = object : ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                getAttestationCommunity().schemaManager.getSchemaNames().sorted()
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return (super.getView(position, convertView, parent) as TextView).apply {
                        text = getAttestationCommunity().schemaManager.getSchemaNames()
                            .sorted()[position]
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    }
                }

                override fun getDropDownView(
                    position: Int,
                    convertView: View?,
                    parent: ViewGroup
                ): View {
                    return (super.getDropDownView(
                        position,
                        convertView,
                        parent
                    ) as TextView).apply {
                        layoutParams.apply {
                            height = resources.getDimensionPixelSize(R.dimen.textViewHeight)
                        }
                        gravity = Gravity.CENTER_VERTICAL
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                        background = if (position == attributeTypeSpinner.selectedItemPosition) {
                            ColorDrawable(Color.LTGRAY)
                        } else {
                            ColorDrawable(Color.WHITE)
                        }
                    }
                }
            }

            attributeTypeSpinner.adapter = attributeNameAdapter
            attributeTypeSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
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
                    val myPeer = getTrustChainCommunity().myPeer

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
                        getAttestationCommunity().requestAttestation(
                            peer,
                            attributeName,
                            privateKey!!,
                            hashMapOf(QRScanController.KEY_ID_FORMAT to idFormat),
                            true
                        )
                    }

                    bottomSheetDialog.dismiss()
                }
            }

            bottomSheetDialog
        }
            ?: throw IllegalStateException(resources.getString(R.string.text_activity_not_null_requirement))
    }
}
