package nl.tudelft.trustchain.datavault.community

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.deserializeVarLen
import nl.tudelft.ipv8.messaging.serializeVarLen
import org.json.JSONArray
import java.nio.charset.Charset

class VaultFileRequestFailedPayload(
    val ids: List<String>,
    val message: String
    ) : Serializable {
    override fun serialize(): ByteArray {
        val idsJsonString = JSONArray(ids).toString()
        return serializeVarLen((idsJsonString).toByteArray()) + serializeVarLen(message.toByteArray())
    }

    companion object Deserializer : Deserializable<VaultFileRequestFailedPayload> {

        override fun deserialize(buffer: ByteArray, offset: Int): Pair<VaultFileRequestFailedPayload, Int> {
            var localOffset = offset

            val (idsJsonString, idsSize) = deserializeVarLen(buffer, localOffset)
            localOffset += idsSize

            val idsJson = JSONArray(idsJsonString.toString(Charset.defaultCharset()))
            val ids = mutableListOf<String>().apply {
                for (i in 0 until idsJson.length()) {
                    add(idsJson.getString(i))
                }
            }

            val (messageBytes, _) = deserializeVarLen(buffer, localOffset)

            return Pair(VaultFileRequestFailedPayload(ids, messageBytes.toString(Charset.defaultCharset())), 0)
        }

    }
}
