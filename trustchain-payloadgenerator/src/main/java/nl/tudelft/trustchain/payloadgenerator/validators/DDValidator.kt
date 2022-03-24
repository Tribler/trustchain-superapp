package nl.tudelft.trustchain.payloadgenerator.validators

import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.TransactionValidator
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.trustchain.payloadgenerator.util.getAmount
import nl.tudelft.trustchain.payloadgenerator.util.getBalance

class DDValidator : TransactionValidator {
    @OptIn(ExperimentalUnsignedTypes::class)
    override fun validate(
        block: TrustChainBlock,
        database: TrustChainStore
    ): ValidationResult {
        // Do not validate offline transactions
        val offline = block.transaction["offline"]?.toString()?.toBoolean()
        if (offline != null && offline) {
            return ValidationResult.Valid
        }

        // Self signed blocks print money, they are always valid
        if (block.isSelfSigned) {
            return ValidationResult.Valid
        }

        val amount = block.getAmount()

        if (block.isProposal) {
            val balance =
                database.getBalance(block.linkPublicKey, block.linkSequenceNumber - 1u)
            return if (balance > amount) {
                ValidationResult.Valid
            } else {
                ValidationResult.Invalid(listOf("Insufficient balance"))
            }
        } else {
            if (block.isAgreement) {
                val balance =
                    database.getBalance(block.publicKey, block.sequenceNumber - 1u)
                return if (balance > amount) {
                    ValidationResult.Valid
                } else {
                    ValidationResult.Invalid(listOf("Insufficient balance"))
                }
            }
        }
        return ValidationResult.Invalid(listOf(""))
    }
}
