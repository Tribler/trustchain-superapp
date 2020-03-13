package nl.tudelft.ipv8.android.demo.coin

import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import org.json.JSONArray
import org.json.JSONObject

object CoinUtil {
    @JvmStatic
    fun parseTransaction(transaction: TrustChainTransaction): JSONObject {
        return JSONObject(transaction["message"].toString())
    }

    @JvmStatic
    fun parseJSONArray(jsonArray: JSONArray): ArrayList<String> {
        return Array(jsonArray.length()) {
            jsonArray.getString(it)
        }.toCollection(ArrayList())
    }
}
