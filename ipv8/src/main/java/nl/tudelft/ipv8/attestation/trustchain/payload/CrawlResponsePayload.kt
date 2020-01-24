package nl.tudelft.ipv8.attestation.trustchain.payload

import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.messaging.*

/**
 * Payload for the response to a crawl request.
 */
open class CrawlResponsePayload(
    val publicKey: ByteArray,
    val sequenceNumber: UInt,
    val linkPublicKey: ByteArray,
    val linkSequenceNumber: UInt,
    val previousHash: ByteArray,
    val signature: ByteArray,
    val blockType: String,
    val transaction: ByteArray,
    val timestamp: ULong,
    val crawlId: UInt,
    val curCount: UInt,
    val totalCount: UInt
) : Serializable {
    override fun serialize(): ByteArray {
        return publicKey +
            serializeUInt(sequenceNumber) +
            linkPublicKey +
            serializeUInt(linkSequenceNumber) +
            previousHash +
            signature +
            serializeVarLen(blockType.toByteArray(Charsets.US_ASCII)) +
            serializeVarLen(transaction) +
            serializeULong(timestamp) +
            serializeUInt(crawlId) +
            serializeUInt(curCount) +
            serializeUInt(totalCount)
    }

    companion object : Deserializable<CrawlResponsePayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<CrawlResponsePayload, Int> {
            var localOffset = 0
            val publicKey = buffer.copyOfRange(offset + localOffset, offset + localOffset + SERIALIZED_PUBLIC_KEY_SIZE)
            localOffset += SERIALIZED_PUBLIC_KEY_SIZE
            val sequenceNumber = deserializeUInt(buffer, offset + localOffset)
            localOffset += SERIALIZED_UINT_SIZE
            val linkPublicKey = buffer.copyOfRange(offset + localOffset, offset + localOffset + SERIALIZED_PUBLIC_KEY_SIZE)
            localOffset += SERIALIZED_PUBLIC_KEY_SIZE
            val linkSequenceNumber = deserializeUInt(buffer, offset + localOffset)
            localOffset += SERIALIZED_UINT_SIZE
            val previousHash = buffer.copyOfRange(offset + localOffset, offset + localOffset + HASH_SIZE)
            localOffset += HASH_SIZE
            val signature = buffer.copyOfRange(offset + localOffset, offset + localOffset + SIGNATURE_SIZE)
            localOffset += SIGNATURE_SIZE
            val (blockType, blockTypeSize) = deserializeVarLen(buffer, offset + localOffset)
            localOffset += blockTypeSize
            val (transaction, transactionSize) = deserializeVarLen(buffer, offset + localOffset)
            localOffset += transactionSize
            val timestamp = deserializeULong(buffer, offset + localOffset)
            localOffset += SERIALIZED_ULONG_SIZE
            val crawlId = deserializeUInt(buffer, offset + localOffset)
            localOffset += SERIALIZED_UINT_SIZE
            val curCount = deserializeUInt(buffer, offset + localOffset)
            localOffset += SERIALIZED_UINT_SIZE
            val totalCount = deserializeUInt(buffer, offset + localOffset)
            localOffset += SERIALIZED_UINT_SIZE
            val payload = CrawlResponsePayload(
                publicKey,
                sequenceNumber,
                linkPublicKey,
                linkSequenceNumber,
                previousHash,
                signature,
                blockType.toString(Charsets.US_ASCII),
                transaction,
                timestamp,
                crawlId,
                curCount,
                totalCount
            )
            return Pair(payload, localOffset)
        }

        fun fromCrawl(block: TrustChainBlock, crawlId: UInt, curCount: UInt, totalCount: UInt): CrawlResponsePayload {
            return CrawlResponsePayload(
                block.publicKey,
                block.sequenceNumber,
                block.linkPublicKey,
                block.linkSequenceNumber,
                block.previousHash,
                block.signature,
                block.type,
                block.transaction,
                block.timestamp.time.toULong(),
                crawlId,
                curCount,
                totalCount
            )
        }
    }
}
