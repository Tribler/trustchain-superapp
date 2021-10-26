package nl.tudelft.trustchain.valuetransfer.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.common.valuetransfer.entity.IdentityAttribute
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import nl.tudelft.trustchain.valuetransfer.util.setNavigationBarColor
import nl.tudelft.trustchain.valuetransfer.util.toggleButton
import java.lang.IllegalStateException

class IdentityAttributeDialog(
    private var attribute: IdentityAttribute?,
) : VTDialogFragment() {

    private var selectedName = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_identity_attribute, null)

            // Fix keyboard exposing over content of dialog
            bottomSheetDialog.behavior.apply {
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
            }

            setNavigationBarColor(requireContext(), parentActivity, bottomSheetDialog)

            val unusedAttributes = getIdentityCommunity().getUnusedAttributeNames()

            val attributeNameSpinner = view.findViewById<Spinner>(R.id.spinnerAttributeName)
            val attributeNameView = view.findViewById<EditText>(R.id.etAttributeName)
            val attributeValueView = view.findViewById<EditText>(R.id.etAttributeValue)
            val saveButton = view.findViewById<Button>(R.id.btnSaveAttribute)
            toggleButton(saveButton, attribute != null)

            attributeNameView.isVisible = attribute != null
            attributeNameSpinner.isVisible = attribute == null

            if (attribute == null) {
                val attributeNameAdapter = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, unusedAttributes) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        return (super.getView(position, convertView, parent) as TextView).apply {
                            text = unusedAttributes[position]
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                        }
                    }

                    override fun getDropDownView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup
                    ): View {
                        return (super.getDropDownView(position, convertView, parent) as TextView).apply {
                            layoutParams.apply {
                                height = resources.getDimensionPixelSize(R.dimen.textViewHeight)
                            }
                            gravity = Gravity.CENTER_VERTICAL
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                            background = if (position == attributeNameSpinner.selectedItemPosition) {
                                ColorDrawable(Color.LTGRAY)
                            } else {
                                ColorDrawable(Color.WHITE)
                            }
                        }
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
                attributeNameView.setText(attribute!!.name)
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
                    getIdentityCommunity().createIdentityAttribute(selectedName, attributeValue).let { newAttribute ->
                        getIdentityStore().addAttribute(newAttribute)

                        parentActivity.displaySnackbar(
                            requireContext(),
                            resources.getString(R.string.snackbar_identity_attribute_add_success)
                        )
                    }
                } else {
                    attribute!!.name = selectedName
                    attribute!!.value = attributeValue

                    getIdentityStore().editAttribute(attribute!!)
                    parentActivity.displaySnackbar(
                        requireContext(),
                        resources.getString(R.string.snackbar_identity_attribute_update_success)
                    )
                }

                activity?.invalidateOptionsMenu()
                bottomSheetDialog.dismiss()
            }

            bottomSheetDialog
        } ?: throw IllegalStateException(resources.getString(R.string.text_activity_not_null_requirement))
    }
}
