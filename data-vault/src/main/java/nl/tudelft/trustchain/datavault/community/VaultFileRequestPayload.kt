package nl.tudelft.trustchain.datavault.community

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.deserializeVarLen
import nl.tudelft.ipv8.messaging.serializeVarLen
import nl.tudelft.trustchain.datavault.accesscontrol.Policy
import org.json.JSONArray
import java.nio.charset.Charset

class VaultFileRequestPayload(
    val ids: List<String>,
    val accessMode: String,
    val accessTokenType: Policy.AccessTokenType,
    val accessTokens: List<String>
) : Serializable {
    override fun serialize(): ByteArray {
        val idsJsonString = JSONArray(ids).toString()
        var serialized = serializeVarLen((idsJsonString).toByteArray()) +
            serializeVarLen(accessMode.toByteArray()) +
            serializeVarLen(accessTokenType.name.toByteArray())

//        could serialize as JSONArray
        accessTokens.forEach {
            serialized += serializeVarLen(it.toByteArray())
        }

        return  serialized
    }

    companion object Deserializer : Deserializable<VaultFileRequestPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<VaultFileRequestPayload, Int> {
            var localOffset = offset

            val (idsJsonString, idsSize) = deserializeVarLen(buffer, localOffset)
            localOffset += idsSize

            val idsJson = JSONArray(idsJsonString.toString(Charset.defaultCharset()))
            val ids = mutableListOf<String>().apply {
                for (i in 0 until idsJson.length()) {
                    add(idsJson.getString(i))
                }
            }

            val (accessMode, accessModeSize) = deserializeVarLen(buffer, localOffset)
            localOffset += accessModeSize

            val (accessTokenTypeBytes, accessTokenTypeSize) = deserializeVarLen(buffer, localOffset)
            localOffset += accessTokenTypeSize

            val accessTokenType = Policy.AccessTokenType.valueOf(accessTokenTypeBytes.toString(Charset.defaultCharset()))
            val accessTokens = mutableListOf<String>()
            while (buffer.lastIndex > localOffset) {
                val (accessToken, atSize) = deserializeVarLen(buffer, localOffset)
                localOffset += atSize
                accessTokens.add(accessToken.toString(Charset.defaultCharset()))
            }

            return Pair(
                VaultFileRequestPayload(
                    ids,
//                    stringId,
                    accessMode.toString(Charset.defaultCharset()),
                    accessTokenType,
                    accessTokens
                ),
                localOffset - offset
            )
        }
    }
}
