package nl.tudelft.trustchain.datavault.community

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.deserializeVarLen
import nl.tudelft.ipv8.messaging.serializeVarLen
import org.json.JSONArray
import java.nio.charset.Charset

class AccessibleFilesPayload(
    val id: String?,
    val sessionToken: String,
    val files: List<String>
): Serializable {
    override fun serialize(): ByteArray {
        var serialized = serializeVarLen((id ?: NULL).toByteArray())
        /*val finalToken = when (sessionToken) {
            null -> NULL
            "" -> NULL
            else -> sessionToken
        }*/
        serialized += serializeVarLen(sessionToken.toByteArray()) + serializeVarLen(JSONArray(files).toString().toByteArray())
        return serialized
    }

    companion object Deserializer : Deserializable<AccessibleFilesPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<AccessibleFilesPayload, Int> {
            var localOffset = offset
            val (id, idSize) = deserializeVarLen(buffer, localOffset)
            localOffset += idSize
            val (token, tokenSize) = deserializeVarLen(buffer, localOffset)
            localOffset += tokenSize
            val (filesBin, _) = deserializeVarLen(buffer, localOffset)

            val files = mutableListOf<String>()
            val jsonFiles = JSONArray(filesBin.toString(Charset.defaultCharset()))
            var i = 0
            while ( i < jsonFiles.length()) {
                files.add(jsonFiles.getString(i))
                i++
            }

            var stringId: String? = id.toString(Charset.defaultCharset())
            if (stringId == NULL) {
                stringId = null
            }

            /*val finalToken: String? = when(token.toString(Charset.defaultCharset())) {
                NULL -> null
                else -> token.toString(Charset.defaultCharset())
            }*/
            return Pair(AccessibleFilesPayload(stringId, token.toString(Charset.defaultCharset()), files), 0)
        }

        const val NULL = "NULL"
    }
}
