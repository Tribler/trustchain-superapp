package nl.tudelft.trustchain.frost

import nl.tudelft.ipv8.messaging.*


class KeyPacketMessage @ExperimentalUnsignedTypes constructor(
    val originPublicKey: ByteArray,
    var ttl: UInt,
    val keyShare: String
) : Serializable {

    override fun serialize(): ByteArray {
        return originPublicKey +
            serializeUInt(ttl) +
            serializeVarLen(keyShare.toByteArray(Charsets.UTF_8))
    }

    @kotlin.ExperimentalUnsignedTypes
    fun checkTTL(): Boolean {
        ttl -= 1u
        if (ttl < 1u) return false
        return true
    }

    companion object Deserializer : Deserializable<KeyPacketMessage> {
        @ExperimentalUnsignedTypes
        @JvmStatic
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<KeyPacketMessage, Int> {
            var localOffset = 0
            val originPublicKey = buffer.copyOfRange(
                offset + localOffset,
                offset + localOffset + SERIALIZED_PUBLIC_KEY_SIZE
            )

            localOffset += SERIALIZED_PUBLIC_KEY_SIZE
            val ttl = deserializeUInt(buffer, offset + localOffset)
            localOffset += SERIALIZED_UINT_SIZE

            val (keyBytes, keySize) = deserializeVarLen(buffer, offset + localOffset)
            val keyShare = keyBytes.toString(Charsets.UTF_8)
            localOffset += keySize

            return Pair(
                first = KeyPacketMessage(
                    originPublicKey = originPublicKey,
                    ttl = ttl,
                    keyShare = keyShare
                ),
                second = localOffset
            )
        }
    }
}
