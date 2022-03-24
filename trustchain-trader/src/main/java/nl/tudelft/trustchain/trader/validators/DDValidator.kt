package nl.tudelft.trustchain.trader.validators

import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.TransactionValidator
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.trustchain.trader.util.getAmount
import nl.tudelft.trustchain.trader.util.getBalance

class DDValidator : TransactionValidator {
    @OptIn(ExperimentalUnsignedTypes::class)
    override fun validate(
        block: TrustChainBlock,
        database: TrustChainStore
    ): ValidationResult {
        // Do not validate offline transactions
        val offline = block.transaction["offline"].toString().toBoolean()
        if (offline) {
            return ValidationResult.Valid
        }

        // Self signed blocks print money, they are always valid
        if (block.isSelfSigned) {
            return ValidationResult.Valid
        }

        val amount = getAmount(block)

        if (block.isProposal) {
            val balance =
                getBalance(block.linkPublicKey, database, block.linkSequenceNumber - 1u)
            return if (balance > amount) {
                ValidationResult.Valid
            } else {
                ValidationResult.Invalid(listOf("Insufficient balance"))
            }
        } else if (block.isAgreement) {
            val balance =
                getBalance(block.publicKey, database, block.sequenceNumber - 1u)
            return if (balance > amount) {
                ValidationResult.Valid
            } else {
                ValidationResult.Invalid(listOf("Insufficient balance"))
            }
        }
        return ValidationResult.Invalid(listOf(""))
    }
}
