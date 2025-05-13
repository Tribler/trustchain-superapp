package nl.tudelft.trustchain.eurotoken.common
import org.json.JSONObject

public class ConnectionData(json: String) : JSONObject(json) {
    val publicKey: String? = this.optString("public_key")
    val amount: Long = this.optLong("amount", -1L)
    val name: String? = this.optString("name")
    val type: String? = this.optString("type")
}
