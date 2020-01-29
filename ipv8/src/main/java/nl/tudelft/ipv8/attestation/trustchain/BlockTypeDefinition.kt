package nl.tudelft.ipv8.attestation.trustchain

import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.ipv8.keyvault.PublicKey

interface BlockTypeDefinition {
    fun createTransaction(
        blockType: String,
        transaction: TrustChainTransaction,
        database: TrustChainStore,
        publicKey: PublicKey,
        link: TrustChainBlock?,
        additionalInfo: Map<String, Any>?,
        linkPk: PublicKey?
    ): TrustChainTransaction

    fun validateTransaction(
        block: TrustChainBlock,
        database: TrustChainStore
    ): ValidationResult
}
