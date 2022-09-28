package nl.tudelft.trustchain.common.ebsi

import android.util.Log
import com.android.volley.Response
import com.android.volley.VolleyError
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.sha256
import nl.tudelft.ipv8.util.toHex
import org.json.JSONArray
import org.json.JSONObject
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.Sign
import org.web3j.crypto.TransactionEncoder
import org.web3j.rlp.RlpEncoder
import org.web3j.rlp.RlpList
import java.math.BigInteger
import java.util.*

object DIDRegistryTools {

    private const val TAG = "DIDTools"

    private fun signRawTransaction(wallet: EBSIWallet, rawTransaction: RawTransaction, chainId: Long): Pair<Sign.SignatureData, ByteArray> {
        val encodedTransaction = TransactionEncoder.encode(rawTransaction, chainId)
        val signatureData = Sign.signMessage(encodedTransaction, wallet.ethCredentials.ecKeyPair)
        val eip155SignatureData =
            TransactionEncoder.createEip155SignatureData(signatureData, chainId)

        val values = TransactionEncoder.asRlpValues(rawTransaction, eip155SignatureData)
        val rlpList = RlpList(values)
        val encoded = RlpEncoder.encode(rlpList)

        return Pair(signatureData, encoded)
    }

    private fun getSignedTransactionRpcPayload(wallet: EBSIWallet, result: JSONObject): JSONObject {
        val signedTransactionRpcPayload = JSONObject()
        signedTransactionRpcPayload.put("jsonrpc", "2.0")
        signedTransactionRpcPayload.put("method", "signedTransaction") // sendSignedTransaction
        signedTransactionRpcPayload.put("id", 1)

        val params = JSONObject().apply {
            put("protocol", "eth")
            put("unsignedTransaction", result)

            // https://docs.web3j.io/4.8.7/transactions/wallet_files/
            val nonce = BigInteger(
                ConformanceTest.removeLeading0x(result.getString("nonce")).hexToBytes())
            val value = BigInteger(
                ConformanceTest.removeLeading0x(result.getString("value")).hexToBytes(true))
            val chainId = BigInteger(
                ConformanceTest.removeLeading0x(result.getString("chainId")).hexToBytes()).toLong()
            val gasLimit = BigInteger(
                ConformanceTest.removeLeading0x(result.getString("gasLimit")).hexToBytes(true))
            val gasPrice = BigInteger(
                ConformanceTest.removeLeading0x(result.getString("gasPrice")).hexToBytes(true))
            val rawTransaction = RawTransaction.createTransaction(
                nonce,
                gasPrice,
                gasLimit,
                result.getString("to"),
                value,
                result.getString("data")
            )

            val signedTransaction = signRawTransaction(wallet, rawTransaction, chainId)
            val signatureData = signedTransaction.first

            put("r", ConformanceTest.addLeading0x(signatureData.r.toHex()))
            put("s", ConformanceTest.addLeading0x(signatureData.s.toHex()))
            put("v", ConformanceTest.addLeading0x(signatureData.v.toHex()))
            put("signedRawTransaction", ConformanceTest.addLeading0x(signedTransaction.second.toHex()))
        }

        signedTransactionRpcPayload.put("params", JSONArray().put(params))

//        Log.e(TAG, "SignedTransaction payload: $signedTransactionRpcPayload")

        return signedTransactionRpcPayload
    }

    private fun getInsertDocumentRpcPayload(wallet: EBSIWallet): JSONObject {
        wallet.did
        val insertDocumentRpcPayload = JSONObject()

        insertDocumentRpcPayload.put("jsonrpc", "2.0")
        insertDocumentRpcPayload.put("method", "insertDidDocument")
        insertDocumentRpcPayload.put("id", 1)

        /*val params = JSONObject().apply {
            val didVersionInfo = VerifiableCredentialsTools.canonicalize(wallet.didDocument(), false)
            val documentHash = sha256(Base64.getUrlEncoder().encode(didVersionInfo.toByteArray()))

            val timestampData = JSONObject().apply {
                put("hash", ConformanceTest.addLeading0x(sha256(documentHash).toHex()))
                put("timestampedBy", wallet.ethCredentials.address)
//                put("blockNumber", 0) //
//                put("transactionHash", "") //
            }

            // hex values with leading 0x
            put("from", wallet.ethCredentials.address)
            put("identifier",
                ConformanceTest.addLeading0x(
                    wallet.did.toByteArray(Charsets.UTF_8).toHex()
                )
            )
            put("hashAlgorithmId", 0)
            put("hashValue", ConformanceTest.addLeading0x(documentHash.toHex()))
            put("didVersionInfo",
                ConformanceTest.addLeading0x(
                    didVersionInfo.toByteArray(Charsets.UTF_8).toHex()
                )
            )
            put("timestampData",
                ConformanceTest.addLeading0x(
                    timestampData.toString().toByteArray(Charsets.UTF_8).toHex()
                )
            )
//                        put("didVersionMetadata", "") //
        }
        insertDocumentRpcPayload.put("params", JSONArray().put(params))*/

//        Log.e("RPC", "insertDidDocument payload: $insertDocumentRpcPayload")
        return insertDocumentRpcPayload
    }

    fun registerDID(wallet: EBSIWallet, errorListener: Response.ErrorListener) {
        Log.e(TAG, "My did: ${wallet.did}")

        val didDocumentListener = Response.Listener<JSONObject> { response ->
            Log.e(TAG, "Did already exists: $response")
            errorListener.onErrorResponse(MyVolleyError("Did already exists"))
        }

        val signedTransactionListener = Response.Listener<JSONObject> { response ->
//            ConformanceTest.printJSONObjectItems(TAG, response)
            val result = ConformanceTest.removeLeading0x(response.getString("result")).hexToBytes().toString(Charsets.UTF_8)
            Log.e(TAG, "Signed transaction result: $result")
        }

        val insertDocumentListener = Response.Listener<JSONObject> { response ->
//            Log.e(TAG, "insertDidDocument response: $response")
            val result = response.getJSONObject("result")
            ConformanceTest.printJSONObjectItems("RegDid Trans", result)

            EBSIAPI.jsonRpc(getSignedTransactionRpcPayload(wallet, result), signedTransactionListener, errorListener)
        }

        val didDocumentErrorListener = Response.ErrorListener {
            if (it.networkResponse.statusCode != 404){
                errorListener.onErrorResponse(MyVolleyError("API error other than 404", it))
                return@ErrorListener
            }

            val responseData = JSONObject(it.networkResponse.data.toString(Charsets.UTF_8))
            if (responseData.getString("title") != "Identifier Not Found") {
                errorListener.onErrorResponse(MyVolleyError("API error other than Identifier not found", it))
                return@ErrorListener
            }

            EBSIAPI.jsonRpc(getInsertDocumentRpcPayload(wallet), insertDocumentListener, errorListener)
        }

        // Check if did is already registered
        EBSIAPI.getDidDocument(wallet.did, didDocumentListener, didDocumentErrorListener)
    }
}
