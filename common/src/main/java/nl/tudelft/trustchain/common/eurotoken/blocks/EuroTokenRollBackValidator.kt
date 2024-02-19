package nl.tudelft.trustchain.common.eurotoken.blocks

import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository

class EuroTokenRollBackValidator(transactionRepository: TransactionRepository) : EuroTokenBaseValidator(transactionRepository) {
    override fun validateEuroTokenProposal(
        block: TrustChainBlock,
        database: TrustChainStore
    ) {
        if (!block.transaction.containsKey(TransactionRepository.KEY_AMOUNT)) {
            throw MissingAmount("Missing amount")
        }
        if (!block.transaction.containsKey(TransactionRepository.KEY_TRANSACTION_HASH)) {
            throw MissingTransactionHash("Missing transaction hash")
        }
        super.validateEuroTokenProposal(block, database)
        val rolledBack =
            database.getBlockWithHash((block.transaction[TransactionRepository.KEY_TRANSACTION_HASH] as String).hexToBytes())
                ?: return
        if (rolledBack.transaction["amount"] != block.transaction["amount"]) {
            throw InvalidTransaction("associated transaction amount does not match")
        }
        return // Valid
    }

    class MissingAmount(message: String) : Invalid(message) {
        override val type: String = "MissingAmount"
    }

    class MissingTransactionHash(message: String) : Invalid(message) {
        override val type: String = "MissingTransactionHash"
    }

    class InvalidTransaction(message: String) : Invalid(message) {
        override val type: String = "InvalidTransaction"
    }
}
