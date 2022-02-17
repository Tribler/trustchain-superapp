package nl.tudelft.trustchain.datavault.ui

import androidx.lifecycle.ViewModel
import nl.tudelft.trustchain.datavault.accesscontrol.Policy
import org.json.JSONObject

class ACMViewModel : ViewModel() {
    private val modifiedPolicies = mutableMapOf<Int, Policy>()
    private var modifiedCredential: Triple<Int, Int, JSONObject>? = null

    fun getModifiedPolicy(policyIndex: Int): Policy? {
        return modifiedPolicies[policyIndex]
    }

    fun setModifiedPolicy(policyIndex: Int, policy: Policy) {
        modifiedPolicies[policyIndex] = policy
    }

    fun clearModifiedPolicies() {
        modifiedPolicies.clear()
    }

    fun setModifiedCredential(policyIndex: Int, credentialIndex: Int, credential: JSONObject) {
        modifiedCredential = Triple(policyIndex, credentialIndex, credential)
    }

    fun clearModifiedCredential() {
        modifiedCredential = null
    }

    fun getModifiedCredential(): Triple<Int, Int, JSONObject>? {
        return  modifiedCredential
    }
}
