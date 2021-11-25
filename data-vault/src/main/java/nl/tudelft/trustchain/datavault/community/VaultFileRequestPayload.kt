package nl.tudelft.trustchain.datavault.community

import android.util.Log
import nl.tudelft.ipv8.attestation.wallet.AttestationBlob
import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.deserializeVarLen
import nl.tudelft.ipv8.messaging.serializeVarLen
import nl.tudelft.ipv8.util.toHex

class VaultFileRequestPayload(
    val id: String,
    val attestations: List<AttestationBlob>
) : Serializable {
    override fun serialize(): ByteArray {
        var serialized = serializeVarLen(id.toByteArray())
        attestations.forEach { attBlob ->
            Log.e("VFRPayload.serialize", attBlob.attestationHash.toHex())
            serialized += serializeVarLen(attBlob.serialize()) }
        return  serialized
    }

    companion object Deserializer : Deserializable<VaultFileRequestPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<VaultFileRequestPayload, Int> {
            var localOffset = offset
            val (id, idSize) = deserializeVarLen(buffer, localOffset)
            localOffset += idSize

            val atts = mutableListOf<AttestationBlob>()
            while (buffer.lastIndex > offset + localOffset) {
                val (attBlobBytes, attBlobSize) = deserializeVarLen(buffer, offset + localOffset)
                localOffset += attBlobSize
                val attBlob = AttestationBlob.Deserializer.deserialize(attBlobBytes, 0).first
                Log.e("VFRPayload.deserialize", attBlob.attestationHash.toHex())
                atts.add(attBlob)
            }

            return Pair(
                VaultFileRequestPayload(
                    id.toString(Charsets.UTF_8),
                    atts
                ),
                localOffset - offset
            )
        }
    }
}
