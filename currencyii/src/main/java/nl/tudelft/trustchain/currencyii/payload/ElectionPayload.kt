package nl.tudelft.trustchain.currencyii.payload

import nl.tudelft.ipv8.messaging.*

class ElectionPayload(
    val DAOid: ByteArray,
) : Serializable {
    override fun serialize(): ByteArray {
        return DAOid
    }

    companion object Deserializer : Deserializable<ElectionPayload> {
        override fun deserialize(
            buffer: ByteArray,
            offset: Int,
        ): Pair<ElectionPayload, Int> {
//            var localOffset = 0
//            val payloadSize = deserializeUShort(buffer, offset)
//            localOffset += SERIALIZED_USHORT_SIZE
//            val DAOId = buffer.copyOfRange(offset + localOffset, offset + localOffset + payloadSize)
//            localOffset += payloadSize
//            return Pair(ElectionPayload(DAOId), localOffset)
            var localOffset = 0
            val (DAOid, DAOidLen) = deserializeRaw(buffer, offset + localOffset)
            localOffset += DAOidLen
            return Pair(ElectionPayload(DAOid), localOffset)
        }
    }
}
