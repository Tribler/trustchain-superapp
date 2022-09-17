package nl.tudelft.trustchain.datavault.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.datavault.R
import nl.tudelft.trustchain.datavault.accesscontrol.AccessControlList
import nl.tudelft.trustchain.datavault.accesscontrol.Policy
import nl.tudelft.trustchain.datavault.accesscontrol.Rules
import nl.tudelft.trustchain.datavault.databinding.AccessControlManagementFragmentBinding
import org.json.JSONObject
import java.io.File

class AccessControlManagementFragment :BaseFragment(R.layout.access_control_management_fragment) {
    private val logTag = "ACMFragment"
    private val binding by viewBinding(AccessControlManagementFragmentBinding::bind)

    private val acmViewModel: ACMViewModel by activityViewModels()
    private val args: AccessControlManagementFragmentArgs by navArgs()
    private lateinit var accessControlList: AccessControlList

    private val policies: List<Policy> get() {
        return accessControlList.getPolicies()
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fileName = args.fileName
        val file = File(fileName)
        accessControlList = AccessControlList(file, null, null)

        val fileSizeMb = file.length() / (1024.0 * 1024)
        binding.fileNameTextView.text = file.name
        binding.fileSizeTextView.text = "%.2f MB".format(fileSizeMb)
        binding.lastModifiedTextView.text = accessControlList.lastModified

        val modifiedCredentialTriple = acmViewModel.getModifiedCredential()

        if (modifiedCredentialTriple != null) {
            val policyIndex = modifiedCredentialTriple.first
            val credentialIndex = modifiedCredentialTriple.second
            val modifiedCredential = modifiedCredentialTriple.third

            val policy = getEditablePolicy(policyIndex)

            policy.rules.also {
                it.updateCredential(credentialIndex, modifiedCredential)
                policy.setRules(it)
            }

            acmViewModel.clearModifiedCredential()
            acmViewModel.setModifiedPolicy(policyIndex, policy)

            Log.d(logTag, "modified credential: $modifiedCredential")
        }

        for (i in policies.indices) {
            val policy = getEditablePolicy(i)
            val policyView = getPolicyView(policy, i)
            binding.policyContainer.addView(policyView)
        }

        binding.cancelButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.saveButton.setOnClickListener {
            savePolicies()
            requireActivity().onBackPressed()
        }
    }

    /***
     * Get the modified version of the policy if available, else the original.
     */
    private fun getEditablePolicy(policyIndex: Int): Policy {
        val policy = acmViewModel.getModifiedPolicy(policyIndex) ?: policies[policyIndex]
        val modifiedActive = acmViewModel.isPolicyActive(policyIndex)
        val active = modifiedActive ?: policy.isActive
        // Log.e(logTag, "Policy $policyIndex active (cache): $modifiedActive, (stored) ${policy.isActive}, (final): $active")
        policy.setActive(active)
        return policy
    }

    private fun savePolicies() {
        val finalPolicies = policies.mapIndexed { index, _ -> getEditablePolicy(index) }
        Log.e(logTag, "final policies: ${finalPolicies.map { p -> p.toString() }}")
        accessControlList.savePolicies(finalPolicies)
    }

    private fun getPolicyView(policy: Policy, index: Int): View {
        val policyView = layoutInflater.inflate(R.layout.policy_layout, null)
        val activeSwitch = policyView.findViewById<Switch>(R.id.activeSwitch)
        val accessModeSpinner = policyView.findViewById<Spinner>(R.id.accessModeSpinner)

        activeSwitch.isChecked = policy.isActive

        activeSwitch.setOnCheckedChangeListener {_, isChecked ->
            acmViewModel.setActivePolicy(index, isChecked)
        }

        ArrayAdapter.createFromResource(requireContext(), R.array.access_modes,  android.R.layout.simple_spinner_item).also {
            accessModeSpinner.adapter = it
            accessModeSpinner.setSelection(it.getPosition(policy.accessMode))
        }

        val ruleContainer = policyView.findViewById<LinearLayout>(R.id.ruleContainer)

        addCredentialViews(ruleContainer, policy.rules, index, 0)

        return policyView
    }

    private fun addCredentialViews(container: ViewGroup, rules: Rules?, policyIndex: Int, leafIndex: Int): Int {
        if (rules == null) return leafIndex

        if (rules.isPublic() || rules.isLeaf()) {
            val credentialView = getCredentialView(rules.credential!!, false, policyIndex, leafIndex)
            container.addView(credentialView)
        } else if(rules.isNegation()) {
            val credentialView = getCredentialView(rules.credential!!, true, policyIndex, leafIndex)
            container.addView(credentialView)
        } else if (rules.isBinaryExpression()) {
            val leftRule = rules.childRule(true)
            val rightRule = rules.childRule(false)

            if (leftRule?.isBinaryExpression() == true) {
                container.removeAllViews()
                container.addView(getOperandView("Invalid rules"))
                return 0
            }
            val leftLeafIndex = addCredentialViews(container, rightRule, policyIndex, leafIndex)
            container.addView(getOperandView(rules.operand!!))
            return addCredentialViews(container, leftRule, policyIndex, leftLeafIndex)
        }

        return leafIndex + 1
    }

    private fun getCredentialView(credential: JSONObject, negation: Boolean, policyIndex: Int, leafIndex: Int): View {
        val credentialContainer = layoutInflater.inflate(R.layout.credential_layout, null) as LinearLayout

        if (negation) {
            credentialContainer.setBackgroundColor(Color.RED)
        }

        for (key in credential.keys()) {
            val entryView = layoutInflater.inflate(R.layout.entry_layout, null)
            val keyTextView = entryView.findViewById<TextView>(R.id.keyTextView)
            val valueTextView = entryView.findViewById<TextView>(R.id.valueTexView)

            keyTextView.text = key

            when (key) {
                Rules.TRUSTED_AUTHORITY -> {
                    valueTextView.text = if (credential.getBoolean(key)) "Yes" else "No"
                }
                else -> valueTextView.text = credential.getString(key)
            }

            credentialContainer.addView(entryView)
        }

        credentialContainer.setOnClickListener {
            // Clear result first
            acmViewModel.clearModifiedCredential()

            // Send policy and leaf index
            val action =
                AccessControlManagementFragmentDirections.actionAccessControlManagementFragmentToCredentialEditorFragment(
                    credential.toString(),
                    true,
                    policyIndex,
                    leafIndex
                )
            findNavController().navigate(action)
        }

        return credentialContainer
    }

    private fun getOperandView(operand: String): View {
        val operandView = TextView(requireContext())
        operandView.text = operand
        return operandView
    }
}
