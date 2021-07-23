package nl.tudelft.trustchain.valuetransfer.dialogs

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.android.synthetic.main.dialog_identity_attribute.*
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.community.IdentityCommunity
import nl.tudelft.trustchain.valuetransfer.db.IdentityStore
import nl.tudelft.trustchain.valuetransfer.entity.Attribute
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import nl.tudelft.trustchain.valuetransfer.util.toggleButton
import java.lang.IllegalStateException
import java.text.SimpleDateFormat
import java.util.*

class IdentityAttributeAddDialog(
    private var attribute: Attribute?,
    private val unusedAttributes: List<String>,
    private val community: IdentityCommunity,
) : DialogFragment() {

    private var selectedName = ""

    private val store by lazy {
        IdentityStore.getInstance(requireContext())
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BaseBottomSheetDialog)
            val view = layoutInflater.inflate(R.layout.dialog_identity_attribute, null)

            val attributeNameSpinner = view.findViewById<Spinner>(R.id.spinnerAttributeName)
            val attributeNameView = view.findViewById<EditText>(R.id.etAttributeName)
            val attributeValueView = view.findViewById<EditText>(R.id.etAttributeValue)
            val saveButton = view.findViewById<Button>(R.id.btnSaveAttribute)
            toggleButton(saveButton, attribute != null)
            val deleteButton = view.findViewById<Button>(R.id.btnDeleteAttribute)
            deleteButton.visibility = if(attribute == null) View.GONE else View.VISIBLE

            attributeNameView.visibility = if(attribute == null) View.GONE else View.VISIBLE
            attributeNameSpinner.visibility = if(attribute == null) View.VISIBLE else View.GONE

            if(attribute == null) {
                val attributeNameAdapter = object: ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, unusedAttributes) {
                    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val textView: TextView = super.getDropDownView(position, convertView, parent) as TextView

                        textView.setPadding(30)

                        if (position == attributeNameSpinner.selectedItemPosition){
                            textView.background = ColorDrawable(Color.LTGRAY)
                        }

                        return textView
                    }
                }

                attributeNameSpinner.adapter = attributeNameAdapter
                attributeNameSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        selectedName = ""
                    }

                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        selectedName = unusedAttributes[position]
                        toggleButton(saveButton, selectedName != "" && attributeValueView.text.isNotEmpty())
                    }
                }
            }else{
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
                    val newAttribute = community.createAttribute(selectedName, attributeValue)
                    store.addAttribute(newAttribute)
                    Toast.makeText(requireContext(), "Attribute added", Toast.LENGTH_SHORT).show()
                } else {
                    attribute!!.name = selectedName
                    attribute!!.value = attributeValue

                    store.editAttribute(attribute!!)
                    Toast.makeText(requireContext(), "Attribute updated", Toast.LENGTH_SHORT).show()
                }

                activity?.invalidateOptionsMenu()
                bottomSheetDialog.dismiss()
            }

            deleteButton.setOnClickListener {
                try {
                    store.deleteAttribute(attribute!!)
                    Toast.makeText(requireContext(), "Attribute deleted", Toast.LENGTH_SHORT).show()

                    activity?.invalidateOptionsMenu()
                    bottomSheetDialog.dismiss()

                } catch(exception: Exception) {
                    Toast.makeText(requireContext(), "Attribute couldn't be deleted", Toast.LENGTH_SHORT).show()
                }
            }

            bottomSheetDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
