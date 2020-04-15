package nl.tudelft.trustchain.trader.validators

import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.TransactionValidator
import nl.tudelft.trustchain.trader.util.getBalance
import nl.tudelft.trustchain.trader.util.getAmount

class DDValidator : TransactionValidator {
    @ExperimentalUnsignedTypes
    override fun validate(
        block: TrustChainBlock,
        database: TrustChainStore
    ): Boolean {
        // Do not validate offline transactions
        val offline = block.transaction["offline"].toString().toBoolean()
        if (offline) {
            return true
        }

        // Self signed blocks print money, they are always valid
        if (block.isSelfSigned) {
            return true
        }

        val amount = getAmount(block)

        if (block.isProposal) {
            val balance =
                getBalance(block.linkPublicKey, database, block.linkSequenceNumber - 1u)
            return balance > amount
        } else if (block.isAgreement) {
            val balance =
                getBalance(block.publicKey, database, block.sequenceNumber - 1u)
            return balance > amount
        }
        return false
    }
}
