package nl.tudelft.ipv8.messaging.payload

import nl.tudelft.ipv8.messaging.*

class BinMemberAuthenticationPayload(
    val publicKey: ByteArray
) : Serializable {
    override fun serialize(): ByteArray {
        return serializeUShort(publicKey.size) + publicKey
    }

    companion object : Deserializable<BinMemberAuthenticationPayload> {
        override fun deserialize(
            buffer: ByteArray,
            offset: Int
        ): Pair<BinMemberAuthenticationPayload, Int> {
            var localOffset = 0
            val payloadSize = deserializeUShort(buffer, offset)
            localOffset += SERIALIZED_USHORT_SIZE
            val publicKey = buffer.copyOfRange(offset + localOffset, offset + localOffset + payloadSize)
            localOffset += payloadSize
            return Pair(BinMemberAuthenticationPayload(publicKey), localOffset)
        }
    }
}
