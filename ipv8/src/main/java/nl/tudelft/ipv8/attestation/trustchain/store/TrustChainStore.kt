package nl.tudelft.ipv8.attestation.trustchain.store

import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock

/**
 * Interface for the persistence layer for the TrustChain Community.
 */
interface TrustChainStore {
    fun addBlock(block: TrustChainBlock)
    fun get(publicKey: ByteArray, sequenceNumber: UInt): TrustChainBlock?
    fun getAllBlocks(): List<TrustChainBlock>
    fun getBlockWithHash(blockHash: ByteArray): TrustChainBlock?
    fun getBlocksWithType(type: String): List<TrustChainBlock>
    fun contains(block: TrustChainBlock): Boolean
    fun getLatest(publicKey: ByteArray, blockType: String? = null): TrustChainBlock?
    fun getLatestBlocks(publicKey: ByteArray, limit: Int = 25, blockTypes: List<String>? = null): List<TrustChainBlock>
    fun getBlockAfter(block: TrustChainBlock, blockType: String? = null): TrustChainBlock?
    fun getBlockBefore(block: TrustChainBlock, blockType: String? = null): TrustChainBlock?
    fun getLowestSequenceNumberUnknown(publicKey: ByteArray): Long
    fun getLowestRangeUnknown(publicKey: ByteArray): LongRange
    fun getLinked(block: TrustChainBlock): TrustChainBlock?
    fun getAllLinked(block: TrustChainBlock): List<TrustChainBlock>
    fun crawl(publicKey: ByteArray, startSeqNum: Long, endSeqNum: Long, limit: Int = 100): List<TrustChainBlock>
    fun getRecentBlocks(limit: Int = 10, offset: Int = 0): List<TrustChainBlock>
    fun getUsers(limit: Int = 100): List<UserInfo>

    /**
     * Returns the list of blocks in which the public key is either a sender or a receiver,
     * sorted by timestamp from the latest.
     *
     * @param publicKey The public key of the peer we are interested in.
     * @param limit The limit of recent blocks to return.
     */
    fun getMutualBlocks(publicKey: ByteArray, limit: Int = 100): List<TrustChainBlock>
}

class UserInfo(
    val publicKey: ByteArray,
    val latestSequenceNumber: Long
)
