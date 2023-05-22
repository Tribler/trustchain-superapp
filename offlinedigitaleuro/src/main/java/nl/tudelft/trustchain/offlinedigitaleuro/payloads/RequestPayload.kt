package nl.tudelft.trustchain.offlinedigitaleuro.payloads

import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex

import android.util.Log

import org.json.JSONObject

class RequestPayload(
    private val pbk: PublicKey,
) {
    fun toJson() :JSONObject {
        val ret = JSONObject()

        ret.put(field_pbk, pbk.keyToBin().toHex())

        return ret
    }

    companion object {
        // public key
        const val field_pbk: String = "PUB_K"

        fun fromJson(json: JSONObject): RequestPayload? {
//            first check if all fields are present
            if (!json.has(field_pbk)) {
                Log.w("offline_money", "In 'RequestPayload::fromJson', json has missing field")
                return null
            }

            return try {
                val pbk: PublicKey = defaultCryptoProvider.keyFromPublicBin(
                    json.getString(field_pbk).hexToBytes()
                )

                RequestPayload(
                    pbk
                )
            } catch (e: Exception) {
                Log.w("offline_money", "Exception in 'RequestPayload::fromJson': $e")
                null
            }
        }
    }
}

