package nl.tudelft.trustchain.datavault.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.switchmaterial.SwitchMaterial
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.datavault.R
import nl.tudelft.trustchain.datavault.accesscontrol.Domain
import nl.tudelft.trustchain.datavault.accesscontrol.Rules
import nl.tudelft.trustchain.datavault.databinding.CredentialEditorLayoutBinding
import org.json.JSONObject

class CredentialEditorFragment: BaseFragment(R.layout.credential_editor_layout) {
    private val binding by viewBinding(CredentialEditorLayoutBinding::bind)
    private val acmViewModel: ACMViewModel by activityViewModels()

    private val args: CredentialEditorFragmentArgs by navArgs()
    private var isEdit = true
    private lateinit var credential: JSONObject
    private val views = mutableMapOf<String, View>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isEdit = args.isEdit
        credential = if (isEdit) JSONObject(args.credential!!) else JSONObject()
        addCredentialView()

        binding.cancelButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.saveButton.setOnClickListener(View.OnClickListener {
            for (entry in views.entries) {
                when (entry.key) {
                    Rules.ATTRIBUTE -> {
                        val valueSpinner = entry.value.findViewById<Spinner>(R.id.valueSpinner)
                        val valueSecondaryEditText = entry.value.findViewById<EditText>(R.id.valueSecondaryEditText)
                        val selectedSchema = valueSpinner.selectedItem as String
                        val attribute = valueSecondaryEditText.text.toString()

                        if (attribute.isEmpty()) {
                            errorMessage("Invalid attribute")
                            return@OnClickListener
                        }

                        val fullAttribute = Domain.formatAttribute(Domain.IGSSI, selectedSchema, attribute)
                        credential.put(Rules.ATTRIBUTE, fullAttribute)

                        if (selectedSchema == nl.tudelft.ipv8.attestation.schema.ID_METADATA_RANGE_18PLUS) {
                            credential.put(Rules.VALUE, nl.tudelft.ipv8.attestation.schema.ID_METADATA_RANGE_18PLUS_PUBLIC_VALUE)
                        }

                        if (selectedSchema == nl.tudelft.ipv8.attestation.schema.ID_METADATA_RANGE_UNDERAGE) {
                            credential.put(Rules.VALUE, nl.tudelft.ipv8.attestation.schema.ID_METADATA_RANGE_UNDERAGE_PUBLIC_VALUE)
                        }
                    }
                    Rules.TRUSTED_AUTHORITY -> {
                        val value = (entry.value as SwitchMaterial).isChecked
                        credential.put(entry.key, value)
                    }
                    else -> {
                        val value = (entry.value as EditText).text.toString()

                        if (value.isEmpty()) {
                            errorMessage("Invalid value ()${entry.key}")
                            return@OnClickListener
                        }

                        credential.put(entry.key, value)
                    }
                }
            }

            Log.e("Saved credential", credential.toString())
            acmViewModel.setModifiedCredential(args.policyIndex, args.credentialIndex, credential)
            findNavController().navigateUp()
        })
    }

    private fun addCredentialView(): View {
        val entryContainer = binding.credentialContainer

        for (key in credential.keys()) {
            val entryView = layoutInflater.inflate(R.layout.entry_layout,null)
            val keyTextView = entryView.findViewById<TextView>(R.id.keyTextView)
            val valueTextView = entryView.findViewById<TextView>(R.id.valueTexView)
            valueTextView.visibility = View.GONE

            val valueEditText = entryView.findViewById<EditText>(R.id.valueEditText)
            val valueSwitch = entryView.findViewById<SwitchMaterial>(R.id.valueSwitch)
            val spinnerGroup = entryView.findViewById<View>(R.id.spinnerGroup)
            val valueSpinner = entryView.findViewById<Spinner>(R.id.valueSpinner)
            val valueSecondaryEditText = entryView.findViewById<EditText>(R.id.valueSecondaryEditText)


            valueSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    disableRangeViews()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    disableRangeViews()
                }
            }

            keyTextView.text = key

            when (key) {
                Rules.ATTRIBUTE -> {
                    val schemas = getSchemas()
                    ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item , schemas).also {
                        valueSpinner.adapter = it
                    }

                    val attributeSplit = credential.getString(key).split(":")
                    val schema = attributeSplit[1]
                    valueSpinner.setSelection(schemas.indexOf(schema))
                    valueSecondaryEditText.setText(attributeSplit.last())
                    spinnerGroup.visibility = View.VISIBLE

                    views[key] = spinnerGroup
                }
                Rules.TRUSTED_AUTHORITY -> {
                    valueSwitch.isChecked = credential.getBoolean(key)
                    valueSwitch.visibility = View.VISIBLE
                    views[key] = valueSwitch
                }
                else -> {
                    valueEditText.setText(credential.getString(key))
                    valueEditText.visibility = View.VISIBLE
                    views[key] = valueEditText
                }
            }

            entryContainer.addView(entryView)
        }

        disableRangeViews()

        return entryContainer
    }

    private fun disableRangeViews() {
        if (Rules.ATTRIBUTE in views && Rules.VALUE in views) {
            val valueSpinner = views[Rules.ATTRIBUTE]!!.findViewById<Spinner>(R.id.valueSpinner)
            val schema = valueSpinner.selectedItem as String

            val valueEditText = views[Rules.VALUE]!! as EditText

            if (schema == nl.tudelft.ipv8.attestation.schema.ID_METADATA_RANGE_18PLUS) {
                valueEditText.setText(nl.tudelft.ipv8.attestation.schema.ID_METADATA_RANGE_18PLUS_PUBLIC_VALUE)
                valueEditText.isEnabled = false
                return
            }

            if (schema == nl.tudelft.ipv8.attestation.schema.ID_METADATA_RANGE_UNDERAGE) {
                valueEditText.setText(nl.tudelft.ipv8.attestation.schema.ID_METADATA_RANGE_UNDERAGE_PUBLIC_VALUE)
                valueEditText.isEnabled = false
                return
            }
        }

        views[Rules.VALUE]?.isEnabled = true
    }

    private fun errorMessage(message: String){
        Log.e("Cred editor", message)
    }

    companion object {
        fun getSchemas(): List<String> {
            return listOf(
                nl.tudelft.ipv8.attestation.schema.ID_METADATA,
                nl.tudelft.ipv8.attestation.schema.ID_METADATA_BIG,
                nl.tudelft.ipv8.attestation.schema.ID_METADATA_HUGE,
                nl.tudelft.ipv8.attestation.schema.ID_METADATA_RANGE_18PLUS,
                nl.tudelft.ipv8.attestation.schema.ID_METADATA_RANGE_UNDERAGE,
            )
        }
    }
}
