package nl.tudelft.trustchain.datavault.accesscontrol

import nl.tudelft.ipv8.attestation.wallet.AttestationBlob
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.valuetransfer.ui.QRScanController
import org.json.JSONArray
import org.json.JSONObject

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
