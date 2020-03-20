package nl.tudelft.ipv8.android.demo.sharedWallet

import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import org.json.JSONObject

abstract class SWBlockTransactionData(jsonData: JSONObject, val blockType: String) {
    val jsonData = jsonData

    fun getJsonString(): String {
        return jsonData.toString()
    }
}
