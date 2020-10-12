package nl.tudelft.trustchain.peerchat.ui.conversation

import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity

class TransactionRepository (
    private val trustChainCommunity: TrustChainCommunity
) {

    fun createSendTransaction(recipient: ByteArray, amount_in_cent: Long): TrustChainBlock {
        val transaction = mapOf(
            KEY_AMOUNT to amount_in_cent
        )
        return trustChainCommunity.createProposalBlock(
            BLOCK_TYPE_TRANSFER, transaction,
            recipient
        )
    }

    fun getTransactionWithHash(hash: ByteArray?): TrustChainBlock? {
        return hash?.let {
            trustChainCommunity.database
                .getBlockWithHash(it)
        }
        }

    companion object {
        private const val BLOCK_TYPE_TRANSFER = "transfer"
        private const val BLOCK_TYPE_CREATE   = "creation"
        private const val BLOCK_TYPE_DESTROY  = "destruction"

        private const val KEY_AMOUNT = "amount"
    }

}
