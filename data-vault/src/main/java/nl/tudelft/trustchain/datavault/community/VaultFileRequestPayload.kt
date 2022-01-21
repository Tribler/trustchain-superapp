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
    val id: String,
    val accessToken: String?,
    val attestations: List<AttestationBlob>?
) : Serializable {
    override fun serialize(): ByteArray {
        var serialized = serializeVarLen(id.toByteArray()) + serializeVarLen((accessToken ?: NULL).toByteArray())
        attestations?.forEach { attBlob ->
            Log.e("VFRPayload.serialize", attBlob.attestationHash.toHex())

            // TEMP REMOVED
            // serialized += serializeVarLen(attBlob.serialize())

            // TEMP ATTESTATION REPLACEMENT
            serialized += serializeVarLen("AGE".toByteArray())
        }
        return  serialized
    }

    companion object Deserializer : Deserializable<VaultFileRequestPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<VaultFileRequestPayload, Int> {
            var localOffset = offset
            val (id, idSize) = deserializeVarLen(buffer, localOffset)
            localOffset += idSize
            val (accessToken, tokenSize) = deserializeVarLen(buffer, localOffset)
            localOffset += tokenSize

            var stringToken: String? = accessToken.toString(Charset.defaultCharset())
            if (stringToken == NULL) {
                stringToken = null
            }

            val atts = mutableListOf<AttestationBlob>()
            while (buffer.lastIndex > localOffset) {
                val (attBlobBytes, attBlobSize) = deserializeVarLen(buffer, localOffset)
                localOffset += attBlobSize
                // TEMP REMOVED
                /*val attBlob = AttestationBlob.Deserializer.deserialize(attBlobBytes, 0).first
                Log.e("VFRPayload.deserialize", attBlob.attestationHash.toHex())
                atts.add(attBlob)*/

                // TEMP ATTESTATION REPLACEMENT
                var tempAtt = attBlobBytes.toString(Charset.defaultCharset())
                if (tempAtt == "AGE") {
                    Log.e("VFRPayload.deserialize", "ATT $tempAtt deserialized")
                    var emptyBytes ="TEST".toByteArray()
                    var attestation = AttestationBlob(emptyBytes, emptyBytes, emptyBytes, "", null, null, null)
                    atts.add(attestation)
                }
            }

            return Pair(
                VaultFileRequestPayload(
                    id.toString(Charset.defaultCharset()),
                    stringToken,
                    atts
                ),
                localOffset - offset
            )
        }

        const val NULL = "NULL"
    }
}
