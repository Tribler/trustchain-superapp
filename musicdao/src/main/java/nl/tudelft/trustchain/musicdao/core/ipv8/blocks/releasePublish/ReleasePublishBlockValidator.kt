package nl.tudelft.trustchain.musicdao.core.ipv8.blocks.releasePublish

import nl.tudelft.trustchain.musicdao.core.ipv8.MusicCommunity
import nl.tudelft.trustchain.musicdao.core.ipv8.blocks.Constants
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.TransactionValidator
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import javax.inject.Inject

class ReleasePublishBlockValidator @Inject constructor(val musicCommunity: MusicCommunity) :
    TransactionValidator {

    override fun validate(block: TrustChainBlock, database: TrustChainStore): ValidationResult {
        return if (validate(block)) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(listOf("Not all information included"))
        }
    }

    private fun validate(block: TrustChainBlock): Boolean {
        return validateTransaction(block.transaction)
    }

    fun validateTransaction(transaction: TrustChainTransaction): Boolean {
        val releaseId = transaction["releaseId"]
        val magnet = transaction["magnet"]
        val title = transaction["title"]
        val artist = transaction["artist"]
        val publisher = transaction["publisher"]
        val releaseDate = transaction["releaseDate"]
        val protocolVersion = transaction["protocolVersion"]

        return (
            releaseId is String && releaseId.isNotEmpty() && transaction.containsKey("releaseId") &&
                magnet is String && magnet.isNotEmpty() && transaction.containsKey("magnet") &&
                title is String && title.isNotEmpty() && transaction.containsKey("title") &&
                artist is String && artist.isNotEmpty() && transaction.containsKey("artist") &&
                publisher is String && publisher.isNotEmpty() && transaction.containsKey("publisher") &&
                releaseDate is String && releaseDate.isNotEmpty() && transaction.containsKey("releaseDate") &&
                protocolVersion is String && protocolVersion.isNotEmpty() && protocolVersion == Constants.PROTOCOL_VERSION
            )
    }

    companion object {
        const val BLOCK_TYPE = ReleasePublishBlock.BLOCK_TYPE
    }
}
