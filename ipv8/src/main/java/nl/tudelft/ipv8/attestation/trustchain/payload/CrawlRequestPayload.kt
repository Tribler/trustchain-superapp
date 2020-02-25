package nl.tudelft.ipv8.attestation.trustchain.payload

import nl.tudelft.ipv8.messaging.*

/**
 * Request a crawl of blocks starting with a specific sequence number or the first if 0.
 */
data class CrawlRequestPayload(
    val publicKey: ByteArray,
    val startSeqNum: Long,
    val endSeqNum: Long,
    val crawlId: UInt
) : Serializable {
    override fun serialize(): ByteArray {
        return publicKey +
            serializeLong(startSeqNum) +
            serializeLong(endSeqNum) +
            serializeUInt(crawlId)
    }

    companion object Deserializer : Deserializable<CrawlRequestPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<CrawlRequestPayload, Int> {
            var localOffset = 0
            val publicKey = buffer.copyOfRange(offset + localOffset,
                offset + localOffset + SERIALIZED_PUBLIC_KEY_SIZE)
            localOffset += SERIALIZED_PUBLIC_KEY_SIZE
            val startSeqNum = deserializeLong(buffer, offset + localOffset)
            localOffset += SERIALIZED_LONG_SIZE
            val endSeqNum = deserializeLong(buffer, offset + localOffset)
            localOffset += SERIALIZED_LONG_SIZE
            val crawlId = deserializeUInt(buffer, offset + localOffset)
            localOffset += SERIALIZED_UINT_SIZE
            val payload = CrawlRequestPayload(publicKey, startSeqNum, endSeqNum, crawlId)
            return Pair(payload, localOffset)
        }
    }
}
