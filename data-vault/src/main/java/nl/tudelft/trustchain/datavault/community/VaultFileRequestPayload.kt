package nl.tudelft.trustchain.datavault.community

import android.util.Log
import nl.tudelft.ipv8.attestation.wallet.AttestationBlob
import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.deserializeVarLen
import nl.tudelft.ipv8.messaging.serializeVarLen
import nl.tudelft.ipv8.util.toHex
import java.nio.charset.Charset

class VaultFileRequestPayload(
    val id: String?,
    val accessMode: String,
    val accessToken: String?,
    val attestations: List<AttestationBlob>?
) : Serializable {
    override fun serialize(): ByteArray {
        var serialized = serializeVarLen((id ?: NULL).toByteArray()) +
            serializeVarLen(accessMode.toByteArray()) +
            serializeVarLen((accessToken ?: NULL).toByteArray())

        attestations?.forEach { attBlob ->
            serialized += serializeVarLen(attBlob.serialize())
        }
        return  serialized
    }

    companion object Deserializer : Deserializable<VaultFileRequestPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<VaultFileRequestPayload, Int> {
            var localOffset = offset
            val (id, idSize) = deserializeVarLen(buffer, localOffset)
            localOffset += idSize
            val (accessMode, accessModeSize) = deserializeVarLen(buffer, localOffset)
            localOffset += accessModeSize
            val (accessToken, tokenSize) = deserializeVarLen(buffer, localOffset)
            localOffset += tokenSize

            var stringId: String? = id.toString(Charset.defaultCharset())
            if (stringId == NULL) {
                stringId = null
            }

            var stringToken: String? = accessToken.toString(Charset.defaultCharset())
            if (stringToken == NULL) {
                stringToken = null
            }

            val atts = mutableListOf<AttestationBlob>()
            while (buffer.lastIndex > localOffset) {
                val (attBlobBytes, attBlobSize) = deserializeVarLen(buffer, localOffset)
                localOffset += attBlobSize
                val attBlob = AttestationBlob.Deserializer.deserialize(attBlobBytes, 0).first
                atts.add(attBlob)
            }

            return Pair(
                VaultFileRequestPayload(
                    stringId,
                    accessMode.toString(Charset.defaultCharset()),
                    stringToken,
                    atts
                ),
                localOffset - offset
            )
        }

        const val NULL = "NULL"
    }
}
