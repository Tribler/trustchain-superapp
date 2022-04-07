package nl.tudelft.trustchain.frost

import nl.tudelft.ipv8.messaging.*


class Ack @ExperimentalUnsignedTypes constructor(
    val originPublicKey: ByteArray,
    val keyShare: ByteArray
) : Serializable {

    override fun serialize(): ByteArray {
        return originPublicKey +
                keyShare
    }

    companion object Deserializer : Deserializable<Ack> {
        @ExperimentalUnsignedTypes
        @JvmStatic
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<Ack, Int> {
            var localOffset = 0
            val originPublicKey = buffer.copyOfRange(
                offset + localOffset,
                offset + localOffset + SERIALIZED_PUBLIC_KEY_SIZE
            )

            localOffset += SERIALIZED_PUBLIC_KEY_SIZE

            val (keyShare, keyShareSize) = deserializeVarLen(buffer, offset + localOffset)
            localOffset += keyShareSize

            return Pair(
                first = Ack(
                    originPublicKey = originPublicKey,
                    keyShare = keyShare
                ),
                second = localOffset
            )
        }
    }
}
