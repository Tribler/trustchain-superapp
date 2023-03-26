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

    init {
        trustChainCommunity.addListener(TOKEN_BLOCK_TYPE, object : BlockListener {
            override fun onBlockReceived(block: TrustChainBlock) {
                logger.debug("Block token: ${block.transaction}, is proposal: ${block.isProposal}, is agreement: ${block.isAgreement}, PK til 8: ${block.publicKey.toString().substring(0, 8)}")
                if (block.isProposal && !block.publicKey.contentEquals(myPublicKey)) {
                    recvTransactionHandler(Transaction.fromTrustChainTransactionObject(block.transaction))
                    agreeTransaction(block)
                } else if (block.isAgreement && !block.publicKey.contentEquals(myPublicKey)) {
                    recvAckHandler(Transaction.fromTrustChainTransactionObject(block.transaction).transactionId)
                }
            }
        })
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

    companion object {
        const val TOKEN_BLOCK_TYPE = "token_block"
    }

}
