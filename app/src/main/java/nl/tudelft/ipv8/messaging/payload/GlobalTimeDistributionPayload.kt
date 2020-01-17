package nl.tudelft.ipv8.messaging.payload

import nl.tudelft.ipv8.messaging.*

data class GlobalTimeDistributionPayload constructor(
    val globalTime: ULong
) : Serializable {
    override fun serialize(): ByteArray {
        return serializeULong(globalTime)
    }

    companion object : Deserializable<GlobalTimeDistributionPayload> {
        override fun deserialize(
            buffer: ByteArray,
            offset: Int
        ): Pair<GlobalTimeDistributionPayload, Int> {
            var localOffset = 0
            val globalTime = deserializeULong(buffer, offset + localOffset)
            localOffset += SERIALIZED_ULONG_SIZE
            return Pair(GlobalTimeDistributionPayload(globalTime), localOffset)
        }
    }
}
