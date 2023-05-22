package nl.tudelft.trustchain.offlinedigitaleuro.payloads

import android.util.Log
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.offlinedigitaleuro.src.Token
import org.json.JSONObject

class TransferQR(
    val pvk: PrivateKey,
    val tokens: MutableSet<Token>,
    private var validity: Boolean = false
) {

    fun getPreviousOwner() : Pair<PublicKey?, String> {
        if (!validity) {
            val (check, errMsg) = checkValidity()
            if (!check) {
                return Pair(null, errMsg)
            }
        }

        for (t in tokens) {
            return getPreviousOwnerOfToken(t)
        }

        return Pair(null, "Error: no tokens into the transaction")
    }

    fun getValue() : Double {
        var sum = 0.0

        for (t in tokens) {
            sum += t.value.toDouble()
        }

        return sum
    }

    // checks if all the tokens are owned by the private key and if all the tokens have
    // the same previous owner
    private fun checkValidity() : Pair<Boolean, String> {
        var prevOwner: PublicKey? = null // the sender of the tokens that also created the intermediary wallet
        val lastRecipientPbk: PublicKey = pvk.pub()

        for (t in tokens) {
            // check that the intermediary wallet is the owner
            if (!(t.lastRecipient contentEquals lastRecipientPbk.keyToBin())) {
                val errMsg = "Error: not all tokens are owned by intermediary wallet"
                Log.d("ODE", errMsg)
                return Pair(false, errMsg)
            }

            // check that the tokens are all owned bt the same previous owner
            if (prevOwner == null) {
                val result = getPreviousOwnerOfToken(t)
                if (result.first == null) {
                    return Pair(false, result.second)
                }

                prevOwner = result.first
            } else {
                val result = getPreviousOwnerOfToken(t)
                if (result.first == null) {
                    return Pair(false, result.second)
                }

                val tokenPrevOwner: PublicKey = result.first!!

                if (!(prevOwner.keyToBin() contentEquals tokenPrevOwner.keyToBin())) {
                    val errMsg = "Error: 2 tokens do not have the same previous owner"
                    Log.d("ODE", errMsg)
                    return Pair(false, errMsg)
                }
            }
        }

        validity = true
        return Pair(true, "")
    }

    private fun getPreviousOwnerOfToken(t: Token): Pair<PublicKey?, String> {
        val prevOwner: PublicKey?

        try {
            val prevOwnerBytePubKey = t.recipients[t.recipients.size - 2].publicKey
            prevOwner = defaultCryptoProvider.keyFromPublicBin(prevOwnerBytePubKey)
        } catch (e: Exception) {
            val errMsg = "Error: could not get sender address from token"
            Log.e("ODE", errMsg)
            return Pair(null, errMsg)
        }

        return Pair(prevOwner, "")
    }

    companion object {
        private const val field_pvk: String = "pvk"
        private const val field_tokens: String = "tokens"

        fun createJson(pvk: PrivateKey, tokens: MutableSet<Token>): JSONObject {
            val ret = JSONObject()

            ret.put(field_pvk, pvk.keyToBin().toHex())
            ret.put(field_tokens, Token.serialize(tokens).toHex())

            return ret
        }

        fun fromJson(json: JSONObject): Pair<TransferQR?, String> {
            try {
                val pvk: PrivateKey = defaultCryptoProvider.keyFromPrivateBin(
                    json.getString(field_pvk).hexToBytes()
                )

                val tokens: MutableSet<Token> = Token.deserialize(json.getString(field_tokens).hexToBytes())

                val ret = TransferQR(pvk, tokens)

                val (status, errMsg) = ret.checkValidity()

                return if (!status) {
                    Pair(null, errMsg)
                } else {
                    Pair(ret, "")
                }
            } catch (e: Exception) {
                val errMsg = "Error: could not construct TransferQR from transaction JSON, reason: $e"
                Log.e("ODE", errMsg)
                return Pair(null, errMsg)
            }
        }
    }
}
