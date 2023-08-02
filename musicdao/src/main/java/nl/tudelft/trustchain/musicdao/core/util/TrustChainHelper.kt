package nl.tudelft.trustchain.musicdao.core.util

import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction

/**
 * A helper class for interacting with TrustChain.
 */
class TrustChainHelper(
    private val trustChainCommunity: TrustChainCommunity
) {

    /**
     * Crawls the chain of the specified peer.
     */
    suspend fun crawlChain(peer: Peer) {
        trustChainCommunity.crawlChain(peer)
    }

    /**
     * Creates a new proposal block, using a text message as the transaction content.
     */
    fun createProposalBlock(
        message: String,
        publicKey: ByteArray,
        blockType: String = "demo_block"
    ) {
        val transaction = mapOf("message" to message)
        trustChainCommunity.createProposalBlock(blockType, transaction, publicKey)
    }

    /**
     * Creates a new proposal block, using a text message as the transaction content.
     */
    fun createProposalBlock(
        transaction: TrustChainTransaction,
        publicKey: ByteArray,
        blockType: String = "demo_block"
    ): TrustChainBlock {
        return trustChainCommunity.createProposalBlock(blockType, transaction, publicKey)
    }

    /**
     * Creates an agreement block to a specified proposal block, using a custom transaction.
     */
    fun createAgreementBlock(link: TrustChainBlock, transaction: TrustChainTransaction) {
        trustChainCommunity.createAgreementBlock(link, transaction)
    }

    /**
     * Returns a list of blocks in which the specified user is participating as a sender or
     * a receiver.
     */
    fun getChainByUser(publicKeyBin: ByteArray): List<TrustChainBlock> {
        return trustChainCommunity.database.getMutualBlocks(publicKeyBin, 1000)
    }
}
