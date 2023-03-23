package nl.tudelft.trustchain.offlinemoney.payloads

import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.ipv8.util.sha256
import nl.tudelft.ipv8.util.toASCII

import android.util.Log

import java.util.Date
import org.json.JSONObject

class Promise(
    val s_pbk: PublicKey,
    val req_payload: RequestPayload,
    val amount: Long,
    val nonce: Long,
    val timestamp: Long,
    val signature: ByteArray,
) {
    fun toJson(): JSONObject {
        val ret = JSONObject()

        ret.put(field_sender_pbk, s_pbk.keyToBin().toHex())
        ret.put(field_request_payload, req_payload.toJson())
        ret.put(field_amount, amount)
        ret.put(field_nonce, nonce)
        ret.put(field_timestamp, timestamp)
        ret.put(field_signature, signature.toHex())

        return ret
    }

    fun checkSignature(): Boolean {
        val transactionHash = getTransactionHash(s_pbk, req_payload, amount, nonce, timestamp)

        return s_pbk.verify(signature, transactionHash)
}

    companion object {
        const val field_sender_pbk = "SEND_PUB_K"
        const val field_request_payload = "REQ_PAYLOAD"
        const val field_amount = "AMOUNT"
        const val field_nonce = "NONCE"
        const val field_timestamp = "TIMESTAMP"
        const val field_signature = "SIGNATURE"

//        create a Promise
        fun createPromise(
            s_pbk: PublicKey,
            req_payload: RequestPayload,
            amount: Long,
            nonce: Long = 0L,
            timestamp: Long = Date().time,
            s_pvk: PrivateKey
        ): Promise {
            val transactionHash = getTransactionHash(s_pbk, req_payload, amount, nonce, timestamp)

            val signature = s_pvk.sign(transactionHash)

            return Promise(
                s_pbk,
                req_payload,
                amount,
                nonce,
                timestamp,
                signature
            )
        }

        fun fromJson(json: JSONObject): Promise? {
//            first check if all fields are present
            if (
                !json.has(field_sender_pbk) || !json.has(field_request_payload)
                || !json.has(field_amount) || !json.has(field_nonce) || !json.has(field_timestamp)
                || !json.has(field_signature)
            ) {
                Log.w("offline_money", "In 'Promise::fromJson', json has missing field")
                return null
            }

            return try {
                val s_pbk: PublicKey = defaultCryptoProvider.keyFromPublicBin(
                    json.getString(field_sender_pbk).hexToBytes()
                )
                val req_payload: RequestPayload = RequestPayload.fromJson(
                    json.getJSONObject(field_request_payload)
                )!!
                val amount: Long = json.getLong(field_amount)
                val nonce: Long = json.getLong(field_nonce)
                val timestamp: Long = json.getLong(field_timestamp)
                val signature: ByteArray = json.getString(field_signature).hexToBytes()

                Promise(
                    s_pbk,
                    req_payload,
                    amount,
                    nonce,
                    timestamp,
                    signature
                )
            } catch (e: Exception) {
                Log.w("offline_money", "Exception in 'Promise::fromJson': $e")
                null
            }
        }

        //        the hash is done only on the transaction info in JSON format for now
        fun getTransactionHash(
            s_pbk: PublicKey,
            req_payload: RequestPayload,
            amount: Long,
            nonce: Long = 0L,
            timestamp: Long = Date().time
        ): ByteArray {
            val transaction = JSONObject()

            transaction.put(field_sender_pbk, s_pbk.keyToBin().toHex())
            transaction.put(field_request_payload, req_payload.toJson()) // introduce it as a JSON
            transaction.put(field_amount, amount)
            transaction.put(field_nonce, nonce)
            transaction.put(field_timestamp, timestamp)

            return sha256(toASCII(transaction.toString()))
        }
    }
}
