package nl.tudelft.trustchain.trader.validators

import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.TransactionValidator
import nl.tudelft.trustchain.trader.util.getAmount
import nl.tudelft.trustchain.trader.util.getBalance

class DDValidator : TransactionValidator {
    @ExperimentalUnsignedTypes
    override fun validate(
        block: TrustChainBlock,
        database: TrustChainStore
    ): Boolean {
        // Do not validate offline transactions
        val offline = block.transaction["offline"]?.toString()?.toBoolean()
        if (offline != null && offline) {
            return true
        }

        // Self signed blocks print money, they are always valid
        if (block.isSelfSigned) {
            return true
        }

        val amount = block.getAmount()

        if (block.isProposal) {
            val balance =
                database.getBalance(block.linkPublicKey, block.linkSequenceNumber - 1u)
            return balance > amount
        } else if (block.isAgreement) {
            val balance =
                database.getBalance(block.publicKey, block.sequenceNumber - 1u)
            return balance > amount
        }
        return false
    }
}
