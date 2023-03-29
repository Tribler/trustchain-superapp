package nl.tudelft.trustchain.detoks_engine.trustchain

import mu.KotlinLogging
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.trustchain.BlockListener
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.trustchain.common.util.TrustChainHelper
import nl.tudelft.trustchain.detoks_engine.manage_tokens.Transaction

class CommunityAdapter(
    private val trustChainCommunity: TrustChainCommunity
) {
    private val trustChainHelper: TrustChainHelper = TrustChainHelper(trustChainCommunity)
    val myPublicKey = trustChainHelper.getMyPublicKey()
    val logger = KotlinLogging.logger("TokenTransaction")
    lateinit var recvAckHandler: ((transactionId: String) -> Unit)
    lateinit var recvTransactionHandler: ((transaction: Transaction) -> Unit)
    private var firstRelevantBlockSeq = -1
    private var tokenPosInBlock = -1
    private var tokenCount = 0

    init {
        trustChainCommunity.addListener(TOKEN_BLOCK_TYPE, object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                handleBlock(block)
            }
        })

        // Find last sent token
        var lastSentToken: String? = null
        var block = trustChainCommunity.database.getLatest(myPublicKey, TOKEN_BLOCK_TYPE)
        while(block != null) {
            // Agreement with my public key is a sent token block
            if (block.isAgreement && !block.isSelfSigned) {
                lastSentToken = Transaction.fromTrustChainTransactionObject(block.transaction).tokens.last()
                break
            }
            block = trustChainCommunity.database.getBlockBefore(block, TOKEN_BLOCK_TYPE)
        }


        // Find all first relevant received token
        block = trustChainCommunity.database.getLatest(myPublicKey, TOKEN_BLOCK_TYPE)
        while (block != null) {
            // Agreement with my public key AS LINK KEY is a rec token block
            if (block.isProposal) {
                if (lastSentToken != null && Transaction.fromTrustChainTransactionObject(block.transaction).tokens.contains(lastSentToken)) {
                    firstRelevantBlockSeq = block.sequenceNumber.toInt()
                    tokenPosInBlock = Transaction.fromTrustChainTransactionObject(block.transaction).tokens.indexOf(lastSentToken)
                    tokenCount += Transaction.fromTrustChainTransactionObject(block.transaction).tokens.size - tokenPosInBlock
                    break
                }
                tokenCount += Transaction.fromTrustChainTransactionObject(block.transaction).tokens.size
            }
            block = trustChainCommunity.database.getBlockBefore(block, TOKEN_BLOCK_TYPE)
        }

        logger.debug("test")
    }

    private fun handleBlock(block: TrustChainBlock) {
        logger.debug("Block token: ${block.transaction}, is proposal: ${block.isProposal}, is agreement: ${block.isAgreement}, PK til 8: ${block.publicKey.toString().substring(0, 8)}")
        if (block.isProposal ) {
            recvTransactionHandler(Transaction.fromTrustChainTransactionObject(block.transaction))
            if (!block.publicKey.contentEquals(myPublicKey)) {
                agreeTransaction(block)
            }
        } else if (block.isAgreement && !block.publicKey.contentEquals(myPublicKey)) {
            recvAckHandler(Transaction.fromTrustChainTransactionObject(block.transaction).transactionId)
        }
    }

    fun proposeTransaction(transaction: Transaction, peer: Peer) {
        logger.debug("Proposing transaction: ${transaction.transactionId} with first token: ${transaction.tokens[0]}")
        trustChainCommunity.createProposalBlock(TOKEN_BLOCK_TYPE, transaction.toTrustChainTransaction(), peer.publicKey.keyToBin())
    }

    fun setReceiveAckHandler(handler: ((transactionId: String) -> Unit)) {
        recvAckHandler = handler
    }

    fun setReceiveTransactionHandler(handler: ((transaction: Transaction) -> Unit)) {
        recvTransactionHandler = handler
    }

    private fun agreeTransaction(proposal: TrustChainBlock) {
        val trustTransaction = proposal.transaction
        val transaction = Transaction.fromTrustChainTransactionObject(trustTransaction)
        logger.debug("Agreeing to transaction: ${transaction.transactionId}")
        trustChainHelper.createAgreementBlock(proposal, trustTransaction)
    }

    fun getPeers(): List<Peer> {
        return trustChainCommunity.getPeers()
    }

//    fun getTokens(): List<String> {
//        trustChainCommunity.database.
//    }

    companion object {
        const val TOKEN_BLOCK_TYPE = "token_block"
    }

}
