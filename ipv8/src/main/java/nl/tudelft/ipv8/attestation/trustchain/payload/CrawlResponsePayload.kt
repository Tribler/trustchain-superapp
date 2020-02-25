package nl.tudelft.ipv8.attestation.trustchain.payload

import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.messaging.*

/**
 * Payload for the response to a crawl request.
 */
data class CrawlResponsePayload(
    val block: HalfBlockPayload,
    val crawlId: UInt,
    val curCount: UInt,
    val totalCount: UInt
) : Serializable {
    override fun serialize(): ByteArray {
        return block.serialize() +
            serializeUInt(crawlId) +
            serializeUInt(curCount) +
            serializeUInt(totalCount)
    }

    companion object Deserializer : Deserializable<CrawlResponsePayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<CrawlResponsePayload, Int> {
            var (block, localOffset) = HalfBlockPayload.deserialize(buffer, offset)
            val crawlId = deserializeUInt(buffer, offset + localOffset)
            localOffset += SERIALIZED_UINT_SIZE
            val curCount = deserializeUInt(buffer, offset + localOffset)
            localOffset += SERIALIZED_UINT_SIZE
            val totalCount = deserializeUInt(buffer, offset + localOffset)
            localOffset += SERIALIZED_UINT_SIZE
            val payload = CrawlResponsePayload(
                block,
                crawlId,
                curCount,
                totalCount
            )
            return Pair(payload, localOffset)
        }

        fun fromCrawl(block: TrustChainBlock, crawlId: UInt, curCount: UInt, totalCount: UInt): CrawlResponsePayload {
            return CrawlResponsePayload(
                HalfBlockPayload.fromHalfBlock(block),
                crawlId,
                curCount,
                totalCount
            )
        }
    }
}
