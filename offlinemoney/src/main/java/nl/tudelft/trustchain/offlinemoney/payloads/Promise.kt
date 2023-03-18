package nl.tudelft.trustchain.offlinemoney.payloads

import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.sha256

import java.util.Date
import org.json.JSONObject

class Promise(
    private val s_pbk: PublicKey,
    private val req_payload: RequestPayload,
    private val amount: Long,
) {
    fun toJson(s_pvk: PrivateKey, nonce: ULong = 0UL, timestamp: Date = Date()): JSONObject {

        var transaction = JSONObject()

        transaction.put(field_sender_pbk, s_pbk)
        transaction.put(field_request_payload, req_payload)
        transaction.put(field_amount, amount)
        transaction.put(field_nonce, nonce)
        transaction.put(field_timestamp, timestamp)

        val transaction_hash = sha256(transaction.toString().hexToBytes())



        return JSONObject()
    }

    companion object {
        const val field_sender_pbk = "SEND_PUB_K"
        const val field_request_payload = "REQ_PAYLOAD"
        const val field_amount = "AMOUNT"
        const val field_nonce = "NONCE"
        const val field_timestamp = "TIMESTAMP"
    }
}
