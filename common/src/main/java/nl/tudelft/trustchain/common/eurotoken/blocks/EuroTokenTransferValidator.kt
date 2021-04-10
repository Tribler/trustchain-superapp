package nl.tudelft.trustchain.common.eurotoken.blocks

import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository

class EuroTokenTransferValidator(transactionRepository: TransactionRepository) : EuroTokenBaseValidator(transactionRepository) {
    override fun validateEuroTokenProposal(block: TrustChainBlock, database: TrustChainStore) {
        if (!block.transaction.containsKey(TransactionRepository.KEY_AMOUNT)) {
            throw MissingAmount("Missing amount")
        }
        super.validateEuroTokenProposal(block, database)
        return // Valid
    }

    class MissingAmount(message: String) : Invalid(message) {
        override val TYPE: String = "MissingAmount"
    }
}
