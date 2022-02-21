package nl.tudelft.trustchain.datavault.ui

import androidx.lifecycle.ViewModel
import nl.tudelft.trustchain.datavault.accesscontrol.Policy
import org.json.JSONObject
import java.io.File

class ACMViewModel : ViewModel() {
    private val activatedPolicies = mutableMapOf<Int, Boolean>()
    private val modifiedPolicies = mutableMapOf<Int, Policy>()
    private var modifiedCredential: Triple<Int, Int, JSONObject>? = null

    fun setActivePolicy(policyIndex: Int, activated: Boolean) {
        activatedPolicies[policyIndex] = activated
    }

    fun isPolicyActive(policyIndex: Int): Boolean? {
        return activatedPolicies[policyIndex]
    }

    fun getModifiedPolicy(policyIndex: Int): Policy? {
        return modifiedPolicies[policyIndex]
    }

    fun setModifiedPolicy(policyIndex: Int, policy: Policy) {
        modifiedPolicies[policyIndex] = policy
    }

    fun clearModifiedPolicies() {
        modifiedPolicies.clear()
        activatedPolicies.clear()
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
