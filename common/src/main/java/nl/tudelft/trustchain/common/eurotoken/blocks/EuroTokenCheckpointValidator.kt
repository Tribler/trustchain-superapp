package nl.tudelft.trustchain.common.eurotoken.blocks

import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.eurotoken.verifyGatewayIdentity

class EuroTokenCheckpointValidator(transactionRepository: TransactionRepository) : EuroTokenBaseValidator(transactionRepository) {
    override fun validateEuroTokenProposal(block: TrustChainBlock, database: TrustChainStore) {
        assertBalanceExists(block)
        // Don't validate balances (this would crawl for previous), we check for this later
        verifyGatewayIdentity(block.linkPublicKey, transactionRepository.gatewayStore)
        // TODO: if mobile can be gateway, check for linked blocks here
        return // Valid
    }
}
