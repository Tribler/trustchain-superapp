package nl.tudelft.ipv8.peerdiscovery.payload

import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.messaging.*
import nl.tudelft.ipv8.messaging.payload.ConnectionType
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex

data class SimilarityRequestPayload(
    val identifier: Int,
    val lanAddress: Address,
    val wanAddress: Address,
    val connectionType: ConnectionType,

    /**
     * The list of service IDs supported by the sender.
     */
    val preferenceList: List<String>
) : Serializable {
    override fun serialize(): ByteArray {
        return serializeUShort(identifier % UShort.MAX_VALUE.toInt()) +
                lanAddress.serialize() +
                wanAddress.serialize() +
                connectionType.serialize() +
                preferenceList.joinToString("").hexToBytes()
    }

    companion object Deserializer : Deserializable<SimilarityRequestPayload> {
        override fun deserialize(
            buffer: ByteArray,
            offset: Int
        ): Pair<SimilarityRequestPayload, Int> {
            var localOffset = 0
            val identifier = deserializeUShort(buffer, offset)
            localOffset += SERIALIZED_USHORT_SIZE
            val (lanAddress, _) = Address.deserialize(buffer, offset + localOffset)
            localOffset += Address.SERIALIZED_SIZE
            val (wanAddress, _) = Address.deserialize(buffer, offset + localOffset)
            localOffset += Address.SERIALIZED_SIZE
            val (connectionType, _) = ConnectionType.deserialize(buffer, offset + localOffset)
            localOffset++
            val preferenceListSerialized = buffer.copyOfRange(offset + localOffset, buffer.size)
            val preferenceList = mutableListOf<String>()
            for (i in 0 until preferenceListSerialized.size / 20) {
                preferenceList += preferenceListSerialized.copyOfRange(20 * i, 20 * i + 20).toHex()
            }
            localOffset += preferenceListSerialized.size
            return Pair(SimilarityRequestPayload(
                identifier,
                lanAddress,
                wanAddress,
                connectionType,
                preferenceList
            ), localOffset)
        }
    }
}
