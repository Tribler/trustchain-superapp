package nl.tudelft.trustchain.musicdao.core.ipv8.blocks.artistAnnounce

import nl.tudelft.trustchain.musicdao.core.ipv8.blocks.Constants
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.TransactionValidator
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import javax.inject.Inject

class ArtistAnnounceBlockValidator @Inject constructor() : TransactionValidator {

    override fun validate(block: TrustChainBlock, database: TrustChainStore): ValidationResult {
        return if (validate(block)) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(listOf("Not all information included."))
        }
    }

    private fun validate(block: TrustChainBlock): Boolean {
        return validateTransaction(block.transaction)
    }

    fun validateTransaction(transaction: TrustChainTransaction): Boolean {
        val publicKey = transaction["publicKey"]
        val bitcoinAddress = transaction["bitcoinAddress"]
        val name = transaction["name"]
        val biography = transaction["biography"]
        val socials = transaction["socials"]
        val protocolVersion = transaction["protocolVersion"]

        return (
            publicKey is String && publicKey.isNotEmpty() && transaction.containsKey("publicKey") &&
                bitcoinAddress is String && bitcoinAddress.isNotEmpty() && transaction.containsKey("bitcoinAddress") &&
                name is String && name.isNotEmpty() && transaction.containsKey("name") &&
                biography is String && biography.isNotEmpty() && transaction.containsKey("biography") &&
                socials is String && socials.isNotEmpty() && transaction.containsKey("socials") &&
                protocolVersion is String && protocolVersion.isNotEmpty() && protocolVersion == Constants.PROTOCOL_VERSION
            )
    }
}
