package nl.tudelft.trustchain.peerchat.community

import nl.tudelft.ipv8.messaging.*

data class AckPayload(
    val id: String
) : Serializable {
    override fun serialize(): ByteArray {
        return serializeVarLen(id.toByteArray())
    }

    companion object Deserializer : Deserializable<AckPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<AckPayload, Int> {
            var localOffset = offset
            val (id, idSize) = deserializeVarLen(buffer, localOffset)
            localOffset += idSize
            return Pair(
                AckPayload(id.toString(Charsets.UTF_8)),
                localOffset - offset
            )
        }
    }
}
