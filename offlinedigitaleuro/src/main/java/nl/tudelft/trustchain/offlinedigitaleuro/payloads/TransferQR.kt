package nl.tudelft.trustchain.offlinedigitaleuro.payloads

import android.util.Log
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.offlinedigitaleuro.src.Token
import org.json.JSONObject

class TransferQR(
    public val pvk: PrivateKey,
    public val tokens: MutableSet<Token>,
) {

    fun createJson(): JSONObject {
        return TransferQR.createJson(pvk, tokens);
    }

    companion object {
        const val field_pvk: String = "pvk";
        const val field_tokens: String = "tokens";

        fun createJson(pvk: PrivateKey, tokens: MutableSet<Token>): JSONObject {
            val ret = JSONObject();

            ret.put(field_pvk, pvk.keyToBin().toHex());
            ret.put(field_tokens, Token.serialize(tokens).toHex());

            return ret;
        }

        fun fromJson(json: JSONObject): TransferQR? {
            var ret: TransferQR?
            try {
                val pvk: PrivateKey = defaultCryptoProvider.keyFromPrivateBin(
                    json.getString(field_pvk).hexToBytes()
                );

                val tokens: MutableSet<Token> = Token.deserialize(json.getString(field_tokens).hexToBytes());

                ret = TransferQR(pvk, tokens);
            } catch (e: Exception) {
                Log.i("TransferQR::fromJSON", e.toString())
                ret = null;
            }

            return ret;
        }
    }
}
