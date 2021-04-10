package nl.tudelft.trustchain.common.eurotoken.blocks

import android.util.Log
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.eurotoken.verifyGatewayIdentity

class EuroTokenCreationValidator(transactionRepository: TransactionRepository) : EuroTokenBaseValidator(transactionRepository) {
    override fun validateEuroTokenProposal(block: TrustChainBlock, database: TrustChainStore) {
        if (!block.transaction.containsKey(TransactionRepository.KEY_AMOUNT)) {
            throw MissingAmount("Missing amount")
        }
        Log.w("EuroTokenBlockCreate", "Is valid proposal")
        verifyGatewayIdentity(block.publicKey, transactionRepository.gatewayStore)
        return // Valid
    }
    class MissingAmount(message: String) : Invalid(message) {
        override val TYPE: String = "MissingAmount"
    }
}
