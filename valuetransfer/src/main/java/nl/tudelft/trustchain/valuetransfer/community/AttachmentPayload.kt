package nl.tudelft.trustchain.valuetransfer.community

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.deserializeVarLen
import nl.tudelft.ipv8.messaging.serializeVarLen

class AttachmentPayload(
    val id: String,
    val data: ByteArray
) : Serializable {
    override fun serialize(): ByteArray {
        return serializeVarLen(id.toByteArray()) +
            serializeVarLen(data)
    }

    companion object Deserializer : Deserializable<AttachmentPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<AttachmentPayload, Int> {
            var localOffset = offset
            val (id, idSize) = deserializeVarLen(buffer, localOffset)
            localOffset += idSize
            val (data, dataSize) = deserializeVarLen(buffer, localOffset)
            localOffset += dataSize
            return Pair(
                AttachmentPayload(
                    id.toString(Charsets.UTF_8),
                    data
                ),
                localOffset - offset
            )
        }
    }
}
