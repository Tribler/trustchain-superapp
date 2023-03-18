package nl.tudelft.trustchain.offlinemoney.payloads

import org.json.JSONObject

import nl.tudelft.ipv8.keyvault.PublicKey

class RequestPayload(
    private val pbk: PublicKey,
) {
    fun toJson() :JSONObject {
        var ret = JSONObject()

        ret.put(field_pbk, pbk)

        return ret
    }

    companion object {
        // public key
        const val field_pbk: String = "PUB_K"
    }
}

