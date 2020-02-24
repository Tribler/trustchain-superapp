package nl.tudelft.ipv8.peerdiscovery.payload

import nl.tudelft.ipv8.messaging.*
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex

data class SimilarityResponsePayload(
    val identifier: Int,

    /**
     * The list of service IDs supported by the sender.
     */
    val preferenceList: List<String>
) : Serializable {
    override fun serialize(): ByteArray {
        return serializeUShort(identifier % UShort.MAX_VALUE.toInt()) +
                serializeUShort(preferenceList.size) +
                preferenceList.joinToString("").hexToBytes()
    }

    companion object Deserializer : Deserializable<SimilarityResponsePayload> {
        override fun deserialize(
            buffer: ByteArray,
            offset: Int
        ): Pair<SimilarityResponsePayload, Int> {
            var localOffset = 0
            val identifier = deserializeUShort(buffer, offset)
            localOffset += SERIALIZED_USHORT_SIZE
            val preferenceListSize = deserializeUShort(buffer, offset + localOffset)
            localOffset += SERIALIZED_USHORT_SIZE
            val preferenceListSerialized = buffer.copyOfRange(offset + localOffset, offset + localOffset + preferenceListSize * 20)
            val preferenceList = mutableListOf<String>()
            for (i in 0 until preferenceListSize) {
                preferenceList += preferenceListSerialized.copyOfRange(20 * i, 20 * i + 20).toHex()
            }
            localOffset += preferenceListSerialized.size
            return Pair(SimilarityResponsePayload(identifier, preferenceList), localOffset)
        }
    }
}
