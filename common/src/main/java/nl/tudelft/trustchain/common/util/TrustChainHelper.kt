package nl.tudelft.trustchain.common.util

import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.ipv8.attestation.trustchain.store.UserInfo
import nl.tudelft.trustchain.common.constants.Currency
import nl.tudelft.trustchain.common.messaging.TradePayload

/**
 * A helper class for interacting with TrustChain.
 */
class TrustChainHelper(
    private val trustChainCommunity: TrustChainCommunity
) {
    /**
     * Returns a list of users and their chain lengths.
     */
    fun getUsers(): List<UserInfo> {
        return trustChainCommunity.database.getUsers()
    }

    /**
     * Returns the number of blocks stored for the given public key.
     */
    fun getStoredBlockCountForUser(publicKeyBin: ByteArray): Long {
        return trustChainCommunity.database.getBlockCount(publicKeyBin)
    }

    /**
     * Returns a peer by its public key if found.
     */
    fun getPeerByPublicKeyBin(publicKeyBin: ByteArray): Peer? {
        return trustChainCommunity.network.getVerifiedByPublicKeyBin(publicKeyBin)
    }

    /**
     * Crawls the chain of the specified peer.
     */
    suspend fun crawlChain(peer: Peer) {
        trustChainCommunity.crawlChain(peer)
    }

    /**
     * Creates a new proposal block, using a text message as the transaction content.
     */
    fun createProposalBlock(message: String, publicKey: ByteArray): TrustChainBlock {
        val blockType = "demo_block"
        val transaction = mapOf("message" to message)
        return trustChainCommunity.createProposalBlock(blockType, transaction, publicKey)
    }
    /**
     * Creates a new proposal block of type "accept_ask_block", using a float as transaction amount.
     */
    fun createAcceptTxProposalBlock(primaryCurrency: Currency, secondaryCurrency: Currency, availableAmount: Float?,
                                    requiredAmount:Float?, type: TradePayload.Type, publicKey: ByteArray): TrustChainBlock {
        val blockType = "demo_tx_block"
        val transaction = mapOf("From" to primaryCurrency.toString(), "Amount from" to availableAmount.toString(),
            "To" to secondaryCurrency.toString(),"Amount to" to requiredAmount.toString(),"type" to type.toString())
        return trustChainCommunity.createProposalBlock(blockType, transaction, publicKey)
    }

    /**
     * Creates a new proposal block of type "demo_tx_block", using a float as transaction amount.
     */
    fun createOfflineTxProposalBlock(amount: Float, publicKey: ByteArray): TrustChainBlock {
        val blockType = "demo_tx_block"
        val transaction = mapOf("amount" to amount, "offline" to true)
        return trustChainCommunity.createProposalBlock(blockType, transaction, publicKey)
    }

    /**
     * Creates a new proposal block of type "demo_tx_block", using a float as transaction amount.
     */
    fun createTxProposalBlock(amount: Float?, publicKey: ByteArray): TrustChainBlock {
        val blockType = "demo_tx_block"
        val transaction = mapOf("amount" to amount)
        return trustChainCommunity.createProposalBlock(blockType, transaction, publicKey)
    }

    /**
     * Creates an agreement block to a specified proposal block, using a custom transaction.
     */
    fun createAgreementBlock(link: TrustChainBlock, transaction: TrustChainTransaction): TrustChainBlock {
        return trustChainCommunity.createAgreementBlock(link, transaction)
    }

    /**
     * Returns a list of blocks in which the specified user is participating as a sender or
     * a receiver.
     */
    fun getChainByUser(publicKeyBin: ByteArray): List<TrustChainBlock> {

        return trustChainCommunity.database.getMutualBlocks(publicKeyBin, 1000)
    }

    fun getBlocksByType(type: String): List<TrustChainBlock>{
        return trustChainCommunity.database.getBlocksWithType(type)
    }

    /**
     * Returns public key of self
     */
    fun getMyPublicKey(): ByteArray {
        return trustChainCommunity.myPeer.publicKey.keyToBin()
    }
}
