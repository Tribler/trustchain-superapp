package nl.tudelft.ipv8.peerdiscovery.payload

import nl.tudelft.ipv8.messaging.*

data class PongPayload(
    val identifier: Int
) : Serializable {
    override fun serialize(): ByteArray {
        return serializeUShort(identifier % UShort.MAX_VALUE.toInt())
    }

    companion object Deserializer : Deserializable<PongPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<PongPayload, Int> {
            val identifier = deserializeUShort(buffer, offset)
            return Pair(PongPayload(identifier), offset + SERIALIZED_USHORT_SIZE)
        }
    }
}
