package nl.tudelft.trustchain.datavault.community

import android.util.Log
import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.deserializeVarLen
import nl.tudelft.ipv8.messaging.serializeVarLen
import nl.tudelft.trustchain.datavault.accesscontrol.Policy
import org.json.JSONArray
import java.nio.charset.Charset

class VaultFileRequestPayload(
//    val id: String?,
    val ids: List<String>,
    val accessMode: String,
    val accessTokenType: Policy.AccessTokenType,
    val accessTokens: List<String>
//    val accessToken: String?,
//    val attestations: List<AttestationBlob>?
) : Serializable {
    override fun serialize(): ByteArray {
        val idsJsonString = JSONArray(ids).toString()
//        var serialized = serializeVarLen((id ?: NULL).toByteArray()) +
        var serialized = serializeVarLen((idsJsonString).toByteArray()) +
            serializeVarLen(accessMode.toByteArray()) +
            serializeVarLen(accessTokenType.name.toByteArray())

        accessTokens.forEach {
            serialized += serializeVarLen(it.toByteArray())
        }
            /*serializeVarLen((accessToken ?: NULL).toByteArray())

        attestations?.forEach { attBlob ->
            serialized += serializeVarLen(attBlob.serialize())
        }*/
        return  serialized
    }

    companion object Deserializer : Deserializable<VaultFileRequestPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<VaultFileRequestPayload, Int> {
            var localOffset = offset
            /*val (id, idSize) = deserializeVarLen(buffer, localOffset)
            localOffset += idSize*/

            val (idsJsonString, idsSize) = deserializeVarLen(buffer, localOffset)
            localOffset += idsSize

            val idsJson = JSONArray(idsJsonString)
            val ids = mutableListOf<String>().apply {
                for (i in 0 until idsJson.length()) {
                    add(idsJson.getString(i))
                }
            }

            val (accessMode, accessModeSize) = deserializeVarLen(buffer, localOffset)
            localOffset += accessModeSize
//            val (accessToken, tokenSize) = deserializeVarLen(buffer, localOffset)
//            localOffset += tokenSize

            val (accessTokenTypeBytes, accessTokenTypeSize) = deserializeVarLen(buffer, localOffset)
            localOffset += accessTokenTypeSize

           /* var stringId: String? = id.toString(Charset.defaultCharset())
            if (stringId == NULL) {
                stringId = null
            }*/

            val accessTokenType = Policy.AccessTokenType.valueOf(accessTokenTypeBytes.toString(Charset.defaultCharset()))
           /* var stringToken: String? = accessToken.toString(Charset.defaultCharset())
            if (stringToken == NULL) {
                stringToken = null
            }*/

            /*val atts = mutableListOf<AttestationBlob>()
            while (buffer.lastIndex > localOffset) {
                val (attBlobBytes, attBlobSize) = deserializeVarLen(buffer, localOffset)
                localOffset += attBlobSize
                val attBlob = AttestationBlob.Deserializer.deserialize(attBlobBytes, 0).first
                atts.add(attBlob)
            }*/

            val accessTokens = mutableListOf<String>()
            while (buffer.lastIndex > localOffset) {
                val (accessToken, atSize) = deserializeVarLen(buffer, localOffset)
                localOffset += atSize
                accessTokens.add(accessToken.toString(Charset.defaultCharset()))
            }

            /*return Pair(
                VaultFileRequestPayload(
                    stringId,
                    accessMode.toString(Charset.defaultCharset()),
                    stringToken,
                    atts
                ),
                localOffset - offset
            )*/

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

        const val NULL = "NULL"
    }
}
