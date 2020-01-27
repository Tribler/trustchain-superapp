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
    fun getBlockAfter(block: TrustChainBlock, blockType: String? = null)
    fun getBlockBefore(block: TrustChainBlock, blockType: String? = null)
}
