package nl.tudelft.ipv8.attestation.trustchain

/**
 * Interface for the persistence layer for the TrustChain Community.
 */
interface TrustChainStore {
    fun addBlock(block: TrustChainBlock)
    fun removeBlock(block: TrustChainBlock)
    fun get(publicKey: ByteArray, sequenceNumber: Int)
    fun getAllBlocks(): List<TrustChainBlock>
    fun getBlockWithHash(blockHash: String): TrustChainBlock?
    fun getBlocksWithType(type: String): List<TrustChainBlock>
    fun contains(block: TrustChainBlock): Boolean
    fun getLatest(publicKey: ByteArray, blockType: String? = null): TrustChainBlock?
    fun getLatestBlocks(publicKey: ByteArray, limit: Int = 25, blockTypes: List<String>? = null)
    fun getBlockAfter(block: TrustChainBlock, blockType: String? = null)
    fun getBlockBefore(block: TrustChainBlock, blockType: String? = null)
    fun getLowerSequenceNumberUnknown(publicKey: ByteArray)
    fun getLowestRangeUnknown(publicKey: ByteArray)
    fun getLinked(block: TrustChainBlock)
    fun getAllLinked(block: TrustChainBlock)
    fun crawl(publicKey: ByteArray, startSeqNumber: Long, endSeqNum: Long, limit: Int = 100): List<TrustChainBlock>
    fun getRecentBlocks(limit: Int = 10, offset: Int = 0)
    fun getUsers(limit: Int = 100)
    fun getConnectedUsers(publicKey: ByteArray, limit: Int = 100)
}
