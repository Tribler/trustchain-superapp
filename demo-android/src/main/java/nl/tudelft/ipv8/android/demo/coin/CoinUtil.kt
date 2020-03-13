package nl.tudelft.ipv8.android.demo.coin

import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import org.json.JSONObject

object CoinUtil {
    @JvmStatic
    fun parseTransaction(transaction: TrustChainTransaction): JSONObject {
        return JSONObject(transaction["message"].toString())
    }
}
