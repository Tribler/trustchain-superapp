package nl.tudelft.trustchain.datavault.accesscontrol

import android.util.Log
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.wallet.AttestationBlob
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity
import nl.tudelft.ipv8.util.sha1
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.valuetransfer.ui.QRScanController
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class AccessControlList(
    private val file: File,
    private val attestationCommunity: AttestationCommunity?
    ) {

    private val logTag = "ACL"
    private var acl: JSONObject? = null // Cached acl. Use getACL()

    fun verifyAccess(peer: Peer, accessMode: String, accessToken: String?, attestations: List<AttestationBlob>?) : Boolean {
        if (!isPublic()) {
            if (accessToken != null) {
                //verify access token
                Log.e(logTag, "Access token")
                return true
            }

            val candidateAttestations = filterAttestations(peer, attestations)
            if (candidateAttestations != null && candidateAttestations.isNotEmpty()){

                val policies = getPolicies()

                for (policy in policies) {
                    if (policy.evaluate(accessMode, candidateAttestations)) return true
                }
            }

            return false
        }

        return true
    }

    private fun filterAttestations(peer: Peer, attestations: List<AttestationBlob>?): List<AttestationBlob>? {

        return attestations?.filter {
            if (it.metadata == null || it.attestorKey == null || it.signature == null) {
                false
            } else {
                val parsedMetadata = JSONObject(it.metadata!!)
                val attesteeKeyHash = parsedMetadata.optString("trustchain_address_hash")
                val isOwner = peer.publicKey.keyToHash().toHex() == attesteeKeyHash
                val isSignatureValid = it.attestorKey!!.verify(it.signature!!,
                    sha1(it.attestationHash + it.metadata!!.toByteArray())
                )

                isOwner && isSignatureValid
            }
        }
    }

    private fun getACL(): JSONObject {
        if (acl == null) {
            /*val aclFile = File(file.absolutePath + ".acl")
            acl = if (aclFile.exists()) {
                try {
                    JSONObject(aclFile.readText())
                } catch (_: Exception) {
                    Log.e(logTag, "Corrupt ACL file: ${file.absolutePath}")
                    newACL()
                }
            } else {
                newACL()
            }*/
            acl = JSONObject(TEST_ACL)
        }

        return acl!!
    }

    fun getPolicies(): List<Policy> {
        return try{
            val policiesJSON = getACL().getJSONArray(POLICIES).noLastNull()
            val policies = mutableListOf<Policy>()

            for (i in 0 until policiesJSON.length()) {
                policies.add(Policy(policiesJSON.getJSONObject(i), attestationCommunity))
            }

            policies
        } catch (_: Exception) {
            Log.e(logTag, "Corrupt ACL file: ${file.absolutePath}")
            listOf()
        }
    }

    private fun newACL(): JSONObject {
        val newACL = JSONObject()
        newACL.put(VERSION, AP_VERSION)
        newACL.put(RESOURCE, file.absolutePath)
        val timestamp = Date().time
        val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        newACL.put(LAST_CHANGED, sdf.format(timestamp))
        newACL.put(POLICIES, JSONArray())
        return newACL
    }

    fun isPublic(): Boolean {
        val acl = File(file.absolutePath + ".acl")
        if (acl.exists()) {
            val aclContent = acl.readText()
            return aclContent == TRUE
        }
        return false
    }

    fun setPublic(isPublic: Boolean) {
        Log.e(logTag, "setPublic: $isPublic")
        /*val acl = File(file.absolutePath + ".acl")
        val fos = FileOutputStream (acl)
        if (isPublic) {
            fos.write(TRUE.toByteArray())
        } else {
            fos.write(FALSE.toByteArray())
        }
        fos.close()*/
    }

    companion object {
        const val AP_VERSION = "1.0"

        const val TRUE = "TRUE"
        const val FALSE = "FALSE"

        const val VERSION = "version"
        const val RESOURCE = "resource "
        const val LAST_CHANGED = "last_changed"
        const val POLICIES = "policies"
        const val MODE = "mode"
        const val RULES = "rules"

        const val TEST_ACL = """{
	"version": 1.0,
	"resource": "photo2.jpg",
	"last_changed": "01-02-2022",
	"policies": [
		{
			"mode": "read",
			"rules": [{"issuer": "IG-SSI:123456789"}]
		},
		{
			"mode": "read+write",
			"rules": [
				[{"attribute": "IG-SSI:id_metadata_range_18plus:ageIG-SSI:id_metadata_range_18plus:age", "value": "18+", "trusted_authority": true}],
				"AND",
				[
                {"attribute": "IG-SSI:id_metadata:city", "value": "Delft", "trusted_authority": false},
                "OR",
                [{"issuer": "IG-SSI:123456789"}]]
			]
		},
	]
}
"""
    }
}

class Policy(
    private var policy: JSONObject,
    private val attestationCommunity: AttestationCommunity?
) {
    private val logTag = "Policy"

    fun evaluate(accessMode: String, attestations: List<AttestationBlob>): Boolean {
        if (!checkAccessMode(accessMode)) return false

        return rules.evaluate(attestations)
    }

    private fun checkAccessMode(requestMode: String): Boolean {
        val policyModes = accessMode.split("+")
        return policyModes.contains(requestMode)
    }

    fun setRules(rules: Rules) {
        policy.put(AccessControlList.RULES, rules.rulesContainer)
    }

    val accessMode: String get() {
        return policy.getString(AccessControlList.MODE)
    }

    val rules: Rules get() {
        return Rules(policy.getJSONArray(AccessControlList.RULES).noLastNull(), attestationCommunity)
    }

    companion object {
        const val READ = "read"
        const val WRITE = "write"
        const val APPEND = "append"
    }
}

class Rules(
    var rulesContainer: JSONArray,
    private val attestationCommunity: AttestationCommunity?
) {
    private val logTag = "Rules"

    fun evaluate(attestations: List<AttestationBlob>): Boolean {
        return when {
            isPublic() -> {
                true
            }
            isLeaf() -> {
                val result = matchCredential(credential!!, attestations)
                result
            }
            isNegation() -> {
                val result = !matchCredential(credential!!, attestations)
                result
            }
            isBinaryExpression() -> {
                val result = when (operand) {
                    AND -> (childRule(true)?.evaluate(attestations) ?: false) &&
                        (childRule(false)?.evaluate(attestations) ?: false)
                    OR -> (childRule(true)?.evaluate(attestations) ?: false) ||
                        (childRule(false)?.evaluate(attestations) ?: false)
                    else -> false
                }
                result
            }
            else -> {
                false
            }
        }
    }

    private fun matchCredential(credential: JSONObject, attestations: List<AttestationBlob>): Boolean{
        val candidateAttestations = attestations.toMutableList()

        if (credential.has(ISSUER)) candidateAttestations.retainAll {
            // {"issuer": "IG-SSI:123456789"}
            it.attestorKey!!.keyToHash().toHex() == credential.getString(ISSUER).split(":").last()
        }

        if (credential.optBoolean(TRUSTED_AUTHORITY, false)) candidateAttestations.retainAll {
            // {"attribute": "IG-SSI:id_metadata_range_18plus:age", "value": "18+", "trusted_authority": true}
            attestationCommunity!!.trustedAuthorityManager.contains(it.attestorKey!!.keyToHash().toHex())
        }

        if (credential.has(ATTRIBUTE)) candidateAttestations.retainAll {
            // {"attribute": "IG-SSI:id_metadata_range_18plus:age", "value": "18+", "trusted_authority": true}
            val parsedMetadata = JSONObject(it.metadata!!)
            val idFormat = parsedMetadata.getString(QRScanController.KEY_ID_FORMAT)
            val attribute = parsedMetadata.getString(QRScanController.KEY_ATTRIBUTE)
            credential.getString(ATTRIBUTE).equals("$IG_SSI:$idFormat:${attribute}", ignoreCase = true)
        }

        if (credential.has(VALUE)) candidateAttestations.retainAll {
            // {"attribute": "IG-SSI:id_metadata_range_18plus:age", "value": "18+", "trusted_authority": true}
            val parsedMetadata = JSONObject(it.metadata!!)
            val value = parsedMetadata.optString(QRScanController.KEY_VALUE)
            value == credential.getString(VALUE)
        }

        return candidateAttestations.isNotEmpty()
    }

    fun updateCredential(index: Int, credential: JSONObject) {
        assert(index < depth)

        val steps = depth - index - 1

        if (steps > 0) {
            if (!isBinaryExpression()) {
                // More steps to take but can go
                return
            }

            val newRules = childRule(false)!!
            newRules.updateCredential(index, credential)
            rulesContainer.put(2, newRules.rulesContainer)
        } else {
            if (isLeaf() || isBinaryExpression()) {
                rulesContainer.put(0, credential)
            } else if (isNegation()) {
                rulesContainer.put(1, credential)
            }
        }
    }

    fun isPublic(): Boolean {
        return rulesContainer.noLastNull().length() == 0
    }

    fun isLeaf(): Boolean {
        return rulesContainer.noLastNull().length() == 1
    }

    fun isNegation(): Boolean {
        return rulesContainer.noLastNull().length() == 2 && rulesContainer.getString(0) == NOT && Rules(toJSONArray(rulesContainer.get(1))!!, null).isLeaf()
    }

    fun isBinaryExpression(): Boolean {
        return rulesContainer.noLastNull().length() == 3
    }

    fun childRule(left: Boolean): Rules? {
        if (isBinaryExpression()) {
            val rule = toJSONArray(rulesContainer.get(if (left) 0 else 2))
            if (rule != null) {
                return Rules(rule, attestationCommunity)
            }
        }

        return null
    }

    val credential: JSONObject? get() {
        return when {
            isLeaf() -> rulesContainer.getJSONObject(0)
            isNegation() -> {
                val credential = rulesContainer.get(1)
                if (credential is JSONArray) credential.getJSONObject(0) else credential as JSONObject
            }
            isPublic() -> {
                val publicRule = JSONObject()
                publicRule.put(PUBLIC, "")
            }
            else -> null
        }
    }

    val operand: String? get() {
        return if (isBinaryExpression()) rulesContainer.getString(1) else null
    }

    val depth: Int get() {
        return when {
            isPublic() -> 0
            isLeaf() -> 1
            isNegation() -> 1
            isBinaryExpression() -> {
                (childRule(true)?.depth ?: 0) + (childRule(false)?.depth ?: 0)
            }
            else -> 0
        }
    }

    companion object {
        const val AND = "AND"
        const val OR = "OR"
        const val NOT = "NOT"

        const val IG_SSI = "IG-SSI"

        const val PUBLIC = "Public"
        const val ATTRIBUTE = "attribute"
        const val VALUE = "value"
        const val TRUSTED_AUTHORITY = "trusted_authority"
        const val ISSUER = "issuer"

        private fun toJSONArray(rule: Any): JSONArray? {
            return when (rule) {
                is JSONArray -> rule
                is JSONObject -> JSONArray().put(rule)
                else -> null
            }
        }
    }

}

fun JSONArray.noLastNull(): JSONArray {
    val size = this.length()
    if (size > 0 && this.isNull(size - 1)) {
        this.remove(size - 1)
    }

    return this
}

class Domain {
    companion object{
        const val IGSSI = "IG-SSI"

        fun formatAttribute(domain: String, schema: String, attribute: String): String {
            return "$domain:$schema:$attribute"
        }
    }
}

/*
{
	"version": 1.0,
	"resource": "photo2.jpg",
	"last_changed": "01-02-2022",
	"policies": [
		{
			"mode": "read+write",
			"rules": [
				{"attribute": "IG-SSI:id_metadata_range_18plus:age", "value": "18+", "trusted_authority": true},
				"AND",
				{"attribute": "IG-SSI:id_metadata:city", "value": "Delft", "trusted_authority": false}
			]
		},
		{
			"mode": "read",
			"rules": [{"issuer": "IG-SSI:123456789"}]
		},
	]
}
 */
