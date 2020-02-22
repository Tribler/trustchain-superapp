package nl.tudelft.ipv8.attestation.trustchain.payload

import nl.tudelft.ipv8.messaging.*

/**
 * Payload for the message that indicates that there are no blocks to respond.
 */
data class EmptyCrawlResponsePayload(
    val crawlId: UInt
) : Serializable {
    override fun serialize(): ByteArray {
        return serializeUInt(crawlId)
    }

    companion object Deserializer : Deserializable<EmptyCrawlResponsePayload> {
        override fun deserialize(
            buffer: ByteArray,
            offset: Int
        ): Pair<EmptyCrawlResponsePayload, Int> {
            val crawlId = deserializeUInt(buffer, offset)
            val payload = EmptyCrawlResponsePayload(crawlId)
            return Pair(payload, SERIALIZED_UINT_SIZE)
        }
    }
}
