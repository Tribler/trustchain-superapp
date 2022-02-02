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

class AccessPolicy(
    private val file: File,
    private val attestationCommunity: AttestationCommunity
    ) {

    private val logTag = "Access Policy"
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
                Log.e(logTag, "Verifying attestations")

                val policies = getPolicies()

                for (i in 0 until policies.length()) {
                    Log.e(logTag, "Evaluating policy ${i+1}/${policies.length()}")
                    val policy = Policy(policies.getJSONObject(i))
                    if (policy.evaluate(accessMode, candidateAttestations, attestationCommunity)) return true
                }
            }

            Log.e(logTag, "No token or attestation")

            return false
        }

        Log.e(logTag, "Public file")

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

    private fun getPolicies(): JSONArray {
        return try{
            val policies = getACL().getJSONArray(POLICIES)
            val size = policies.length()
            if (size > 0 && policies.isNull(size - 1)) {
                policies.remove(size - 1)
            }
            policies
        } catch (_: Exception) {
            Log.e(logTag, "Corrupt ACL file: ${file.absolutePath}")
            JSONArray()
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
				{"attribute": "IG-SSI:id_metadata_range_18plus:age", "value": "18+", "trusted_authority": true},
				"AND",
				{"attribute": "IG-SSI:id_metadata:city", "value": "Delft", "trusted_authority": false}
			]
		},
	]
}
"""
    }
}

class Policy(
    private val policy: JSONObject,
) {
    private val logTag = "Policy"

    fun evaluate(accessMode: String, attestations: List<AttestationBlob>, attestationCommunity: AttestationCommunity): Boolean {
        val policyMode = policy.getString(AccessPolicy.MODE)
        Log.e(logTag, "Evaluating policy ($policyMode) [access mode: $accessMode]")

        if (!checkAccessMode(accessMode, policyMode)) return false

        val rules = policy.getJSONArray(AccessPolicy.RULES)
        return Rules(rules, attestations, attestationCommunity).evaluate()
    }

    private fun checkAccessMode(requestMode: String, policyMode: String): Boolean {
        val policyModes = policyMode.split("+")
        return policyModes.contains(requestMode)
    }

    companion object {
        const val READ = "read"
        const val WRITE = "write"
        const val APPEND = "append"
    }
}

class Rules(
    private val rules: JSONArray,
    private val attestations: List<AttestationBlob>,
    private val attestationCommunity: AttestationCommunity
) {
    private val logTag = "Rules"

    fun evaluate(): Boolean {
        return when {
            isPublic() -> {
                Log.e(logTag, "Public rule")
                true
            }
            isLeaf() -> {
                val credential = rules.getJSONObject(0)
                val result = matchCredential(credential)
                Log.e(logTag, "Evaluated credential: $credential [$result]")
                result
            }
            isNegation() -> {
                val credential = rules.getJSONObject(1)
                val result = !matchCredential(credential)
                Log.e(logTag, "Evaluated negated credential: $credential [$result]")
                result
            }
            isBinaryExpression() -> {
                val operand = rules.getString(1)
                val result = when (operand) {
                    AND -> (childRule(true)?.evaluate() ?: false) &&
                        (childRule(false)?.evaluate() ?: false)
                    OR -> (childRule(true)?.evaluate() ?: false) ||
                        (childRule(false)?.evaluate() ?: false)
                    else -> false
                }
                Log.e(logTag, "Evaluated $operand binary expression: [$result]")
                result
            }
            else -> {
                Log.e(logTag, "Invalid rule")
                false
            }
        }
    }

    private fun matchCredential(credential: JSONObject): Boolean{
        val candidateAttestations = attestations.toMutableList()

        Log.e(logTag, "${candidateAttestations.size} attestation(s)")

        if (credential.has(ISSUER)) candidateAttestations.retainAll {
            // {"issuer": "IG-SSI:123456789"}
            // Log.e(logTag, "issuer match: att[${it.attestorKey!!.keyToHash().toHex()}] == ${credential.getString(ISSUER).split(":").last()}")
            it.attestorKey!!.keyToHash().toHex() == credential.getString(ISSUER).split(":").last()
        }

        if (credential.optBoolean(TRUSTED_AUTHORITY, false)) candidateAttestations.retainAll {
            // {"attribute": "IG-SSI:id_metadata_range_18plus:age", "value": "18+", "trusted_authority": true}
            // Log.e(logTag, "trusted auth match: att[${attestationCommunity.trustedAuthorityManager.contains(it.attestorKey!!.keyToHash().toHex())}]")
            attestationCommunity.trustedAuthorityManager.contains(it.attestorKey!!.keyToHash().toHex())
        }

        if (credential.has(ATTRIBUTE)) candidateAttestations.retainAll {
            // {"attribute": "IG-SSI:id_metadata_range_18plus:age", "value": "18+", "trusted_authority": true}
            val parsedMetadata = JSONObject(it.metadata!!)
            val idFormat = parsedMetadata.getString(QRScanController.KEY_ID_FORMAT)
            val attribute = parsedMetadata.getString(QRScanController.KEY_ATTRIBUTE)
            // Log.e(logTag, "attributed match: att[${attribute.toUpperCase()}] == ${credential.getString(ATTRIBUTE).toUpperCase()}")
            credential.getString(ATTRIBUTE).toUpperCase() == "$IG_SSI:$idFormat:${attribute}".toUpperCase()
        }

        if (credential.has(VALUE)) candidateAttestations.retainAll {
            // {"attribute": "IG-SSI:id_metadata_range_18plus:age", "value": "18+", "trusted_authority": true}
            val parsedMetadata = JSONObject(it.metadata!!)
            val value = parsedMetadata.optString(QRScanController.KEY_VALUE)
            // Log.e(logTag, "value match: att[$value] == ${credential.getString(VALUE)}")
            value == credential.getString(VALUE)
        }

        Log.e(logTag, "attestations matched? ${candidateAttestations.isNotEmpty()}")
        return candidateAttestations.isNotEmpty()
    }

    private fun isPublic(): Boolean {
        return rules.length() == 0
    }

    private fun isLeaf(): Boolean {
        return rules.length() == 1
    }

    private fun isNegation(): Boolean {
        return rules.length() == 2 && rules.getString(0) == NOT
    }

    private fun isBinaryExpression(): Boolean {
        return rules.length() == 3
    }

    private fun childRule(left: Boolean): Rules? {
        if (isBinaryExpression()) {
            val rule = toJSONArray(rules.get(if (left) 0 else 2))
            if (rule != null) {
                return Rules(rule, attestations, attestationCommunity)
            }
        }

        return null
    }

    companion object {
        const val AND = "AND"
        const val OR = "OR"
        const val NOT = "NOT"

        const val IG_SSI = "IG-SSI"

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
