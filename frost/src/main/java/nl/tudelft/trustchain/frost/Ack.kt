package nl.tudelft.trustchain.frost

import nl.tudelft.ipv8.messaging.*


class Ack constructor(
    val keyShare: ByteArray
) : Serializable {
    override fun serialize(): ByteArray {
        return serializeVarLen(keyShare)
    }

    companion object Deserializer : Deserializable<Ack> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<Ack, Int> {
            var localOffset = offset
            val (keyShare, size) = deserializeVarLen(buffer, localOffset)
            localOffset += size
            return Pair(
                Ack(keyShare),
                localOffset - offset
            )
        }
    }
}
