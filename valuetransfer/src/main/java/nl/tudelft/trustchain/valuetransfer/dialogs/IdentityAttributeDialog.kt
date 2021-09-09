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
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.community.IdentityCommunity
import nl.tudelft.trustchain.valuetransfer.db.IdentityStore
import nl.tudelft.trustchain.valuetransfer.entity.IdentityAttribute
import nl.tudelft.trustchain.valuetransfer.util.toggleButton
import java.lang.IllegalStateException

class IdentityAttributeDialog(
    private var attribute: IdentityAttribute?,
) : DialogFragment() {

    private lateinit var parentActivity: ValueTransferMainActivity
    private lateinit var identityCommunity: IdentityCommunity
    private lateinit var identityStore: IdentityStore

    private var selectedName = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_identity_attribute, null)

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.behavior.skipCollapsed = true
            bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

            parentActivity = requireActivity() as ValueTransferMainActivity
            identityCommunity = parentActivity.getCommunity()!!
            identityStore = parentActivity.getStore()!!

            val unusedAttributes = identityCommunity.getUnusedAttributeNames()

            val attributeNameSpinner = view.findViewById<Spinner>(R.id.spinnerAttributeName)
            val attributeNameView = view.findViewById<EditText>(R.id.etAttributeName)
            val attributeValueView = view.findViewById<EditText>(R.id.etAttributeValue)
            val saveButton = view.findViewById<Button>(R.id.btnSaveAttribute)
            toggleButton(saveButton, attribute != null)

            attributeNameView.visibility = if (attribute == null) View.GONE else View.VISIBLE
            attributeNameSpinner.visibility = if (attribute == null) View.VISIBLE else View.GONE

            if (attribute == null) {
                val attributeNameAdapter = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, unusedAttributes) {
                    override fun getDropDownView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup
                    ): View {
                        val textView: TextView = super.getDropDownView(position, convertView, parent) as TextView
                        val params = textView.layoutParams
                        params.height = resources.getDimensionPixelSize(R.dimen.textViewHeight)
                        textView.layoutParams = params
                        textView.gravity = Gravity.CENTER_VERTICAL

                        if (position == attributeNameSpinner.selectedItemPosition) {
                            textView.background = ColorDrawable(Color.LTGRAY)
                        }

                        return textView
                    }
                }

                attributeNameSpinner.adapter = attributeNameAdapter
                attributeNameSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        selectedName = ""
                    }

                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        selectedName = unusedAttributes[position]
                        toggleButton(saveButton, selectedName != "" && attributeValueView.text.isNotEmpty())
                    }
                }
            } else {
                attributeNameView.setText(attribute!!.name.toString())
                attributeValueView.setText(attribute!!.value)

                selectedName = attribute!!.name
            }

            attributeValueView.doAfterTextChanged { state ->
                toggleButton(saveButton, state != null && state.isNotEmpty())
            }

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            saveButton.setOnClickListener {
                val attributeValue = attributeValueView.text.toString()

                if (attribute == null) {
                    val newAttribute = identityCommunity.createAttribute(selectedName, attributeValue)
                    identityStore.addAttribute(newAttribute)
                    parentActivity.displaySnackbar(requireContext(), "Identity attribute added")
                } else {
                    attribute!!.name = selectedName
                    attribute!!.value = attributeValue

                    identityStore.editAttribute(attribute!!)
                    parentActivity.displaySnackbar(requireContext(), "Identity attribute updated")
                }

                activity?.invalidateOptionsMenu()
                bottomSheetDialog.dismiss()
            }

            bottomSheetDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
