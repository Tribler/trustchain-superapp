package nl.tudelft.trustchain.musicdao.core.ipv8.blocks.donation

import nl.tudelft.trustchain.musicdao.core.ipv8.MusicCommunity
import nl.tudelft.ipv8.attestation.trustchain.BlockSigner
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.TransactionValidator
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import javax.inject.Inject

data class DonateTransaction(
    val transactionId: String,
    val toBitcoinAddress: String,
    val fromBitcoinAddress: String,
    val toTrustchainAddress: String,
    val fromTrustchainAddress: String,
    val value: String
) {
    companion object {
        const val block_type = "donation"

        fun toTrustchainTransaction(donateTransaction: DonateTransaction): TrustChainTransaction {
            val map = mutableMapOf<String, Any>()
            map["transactionId"] = donateTransaction.transactionId
            map["toBitcoinAddress"] = donateTransaction.toBitcoinAddress
            map["fromBitcoinAddress"] = donateTransaction.fromBitcoinAddress
            map["toTrustchainAddress"] = donateTransaction.toTrustchainAddress
            map["fromTrustchainAddress"] = donateTransaction.fromTrustchainAddress
            map["value"] = donateTransaction.value
            return map
        }

        fun fromTrustchainTransaction(trustchainTransaction: TrustChainTransaction): DonateTransaction {
            val map = trustchainTransaction
            return DonateTransaction(
                map["transactionId"] as String,
                map["toBitcoinAddress"] as String,
                map["fromBitcoinAddress"] as String,
                map["toTrustchainAddress"] as String, map["fromTrustchainAddress"] as String,
                map["value"] as String,
            )
        }

        private fun validateTransaction(transaction: TrustChainTransaction): Boolean {
            val map = transaction as? Map<*, *> ?: return false
            if (map["transactionId"] !is String) return false
            if (map["toBitcoinAddress"] !is String) return false
            if (map["fromBitcoinAddress"] !is String) return false
            if (map["toTrustchainAddress"] !is String) return false
            if (map["fromTrustchainAddress"] !is String) return false
            if (map["value"] !is String) return false
            return true
        }

        fun validateBlock(block: TrustChainBlock): Boolean {
            val transaction = block.transaction
            return validateTransaction(transaction)
        }

        class Validator : TransactionValidator {
            override fun validate(
                block: TrustChainBlock,
                database: TrustChainStore
            ): ValidationResult {
                val validation = validateBlock(block)
                return if (validation) {
                    ValidationResult.Valid
                } else {
                    ValidationResult.Invalid(listOf("Not all information included"))
                }
            }
        }

        class Signer @Inject constructor(val musicCommunity: MusicCommunity) : BlockSigner {
            override fun onSignatureRequest(block: TrustChainBlock) {
                musicCommunity.createAgreementBlock(block, mapOf<Any?, Any?>())
            }
        }

        // create a new proposal block
        class Repository @Inject constructor(val musicCommunity: MusicCommunity) {

            fun createDonationTransactionMessage(
                transactionId: String,
                toBitcoinAddress: String,
                fromBitcoinAddress: String,
                toTrustchainAddress: String,
                fromTrustchainAddress: String,
                value: String
            ): TrustChainBlock {
                return createDonateTransaction(
                    transactionId = transactionId,
                    toBitcoinAddress,
                    fromBitcoinAddress,
                    toTrustchainAddress,
                    fromTrustchainAddress,
                    value
                )
            }

            private fun createDonateTransaction(
                transactionId: String,
                toBitcoinAddress: String,
                fromBitcoinAddress: String,
                toTrustchainAddress: String,
                fromTrustchainAddress: String,
                value: String
            ): TrustChainBlock {
                val transaction = DonateTransaction(
                    transactionId = transactionId,
                    toBitcoinAddress = toBitcoinAddress,
                    fromBitcoinAddress = fromBitcoinAddress,
                    toTrustchainAddress = toTrustchainAddress,
                    fromTrustchainAddress = fromTrustchainAddress,
                    value = value
                )
                val trustchainTransaction = toTrustchainTransaction(transaction)
                return musicCommunity.createProposalBlock(
                    block_type,
                    trustchainTransaction,
                    musicCommunity.myPeer.publicKey.keyToBin()
                )
            }
        }
    }
}
