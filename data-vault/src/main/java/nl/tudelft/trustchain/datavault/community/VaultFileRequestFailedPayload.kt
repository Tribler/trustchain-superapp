package nl.tudelft.trustchain.datavault.community

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.deserializeVarLen
import nl.tudelft.ipv8.messaging.serializeVarLen
import org.json.JSONArray
import java.nio.charset.Charset

class VaultFileRequestFailedPayload(
//    val id: String?,
    val ids: List<String>,
    val message: String
    ) : Serializable {
    override fun serialize(): ByteArray {
        val idsJsonString = JSONArray(ids).toString()
//        var serialized = serializeVarLen((id ?: AccessibleFilesPayload.NULL).toByteArray())
//        serialized += serializeVarLen(message.toByteArray())
        return serializeVarLen((idsJsonString).toByteArray()) + serializeVarLen(message.toByteArray())
    }

    companion object Deserializer : Deserializable<VaultFileRequestFailedPayload> {
//        const val NULL = "NULL"

        override fun deserialize(buffer: ByteArray, offset: Int): Pair<VaultFileRequestFailedPayload, Int> {
            var localOffset = offset
//            val (id, idSize) = deserializeVarLen(buffer, localOffset)
//            localOffset += idSize

            val (idsJsonString, idsSize) = deserializeVarLen(buffer, localOffset)
            localOffset += idsSize

            val idsJson = JSONArray(idsJsonString)
            val ids = mutableListOf<String>().apply {
                for (i in 0 until idsJson.length()) {
                    add(idsJson.getString(i))
                }
            }

            val (messageBytes, _) = deserializeVarLen(buffer, localOffset)

//            var stringId: String? = id.toString(Charset.defaultCharset())
//            if (stringId == AccessibleFilesPayload.NULL) {
//                stringId = null
//            }

            return Pair(VaultFileRequestFailedPayload(ids, messageBytes.toString(Charset.defaultCharset())), 0)
        }

    }
}
