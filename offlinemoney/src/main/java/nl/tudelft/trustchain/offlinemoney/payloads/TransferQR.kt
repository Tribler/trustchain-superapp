package nl.tudelft.trustchain.offlinemoney.payloads

import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.offlinemoney.src.Token
import org.json.JSONArray
import org.json.JSONObject
import java.util.Vector

class TransferQR {
    companion object {
        const val field_pvk: String = "pvk";
        const val field_tokens: String = "tokens";

        fun createJson(pvk: PrivateKey, tokens: Vector<Token>): JSONObject {
            val ret: JSONObject = JSONObject();

            ret.put(field_pvk, pvk.keyToBin().toHex());
            ret.put(field_tokens, JSONArray(Token.serialize(tokens)));

            return ret;
        }
//
//        fun fromJson(json: JSONObject):  {
//
//        }
    }
}
