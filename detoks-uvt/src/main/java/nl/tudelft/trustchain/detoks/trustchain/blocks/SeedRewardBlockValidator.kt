package nl.tudelft.trustchain.detoks.trustchain.blocks

import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.TransactionValidator
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult

class SeedRewardBlockValidator : TransactionValidator {
    override fun validate(block: TrustChainBlock, database: TrustChainStore): ValidationResult {
       return ValidationResult.Valid
    }
}
