package nl.tudelft.trustchain.currencyii.payload

import nl.tudelft.ipv8.messaging.*

class ElectedPayload(
    val DAOid: ByteArray,
) : Serializable {
    override fun serialize(): ByteArray {
        return DAOid
    }

    companion object Deserializer : Deserializable<ElectedPayload> {
        override fun deserialize(
            buffer: ByteArray,
            offset: Int,
        ): Pair<ElectedPayload, Int> {
//            var localOffset = 0
//            val payloadSize = deserializeUShort(buffer, offset)
//            localOffset += SERIALIZED_USHORT_SIZE
//            val publicKey = buffer.copyOfRange(offset + localOffset, offset + localOffset + payloadSize)
//            localOffset += payloadSize
            var localOffset = 0
            val (publicKey, publicKeyLen) = deserializeRaw(buffer, offset + localOffset)
            localOffset += publicKeyLen
            return Pair(ElectedPayload(publicKey), localOffset)
        }
    }
}
