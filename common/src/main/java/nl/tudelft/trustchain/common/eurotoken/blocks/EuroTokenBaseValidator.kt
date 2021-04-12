package nl.tudelft.trustchain.common.eurotoken.blocks

import android.util.Log
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.BlockRange
import nl.tudelft.ipv8.attestation.trustchain.validation.TransactionValidator
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.eurotoken.getBalanceChangeForBlock
import nl.tudelft.trustchain.common.eurotoken.getBalanceForBlock
import nl.tudelft.trustchain.common.eurotoken.getVerifiedBalanceForBlock

@OptIn(ExperimentalUnsignedTypes::class)
open class EuroTokenBaseValidator(val transactionRepository: TransactionRepository) : TransactionValidator {

    private fun getBlockBeforeOrRaise(block: TrustChainBlock, database: TrustChainStore): TrustChainBlock? {
        if (block.isGenesis) {
            return null
        }

        return database.getBlockWithHash(block.previousHash)
            ?: throw PartialPrevious("Missing previous block")
    }

    private fun verifyListedBalance(block: TrustChainBlock, database: TrustChainStore) {
        assertBalanceExists(block)
        val blockBefore = getBlockBeforeOrRaise(block, database)
        val balanceBefore = if (blockBefore != null) getBalanceForBlock(blockBefore, database) else 0L
        balanceBefore ?: throw PartialPrevious("Missing previous block")
        val balanceChange = getBalanceChangeForBlock(block)
        if ((block.transaction[TransactionRepository.KEY_BALANCE] as Long) < 0L) {
            throw InsufficientBalance("block balance (${block.sequenceNumber}): ${block.transaction[TransactionRepository.KEY_BALANCE]} is negative")
        }
        if (block.transaction[TransactionRepository.KEY_BALANCE] != balanceBefore + balanceChange) {
            Log.w("EuroTokenBlock", "Invalid balance")
            throw InvalidBalance("block balance (${block.sequenceNumber}): ${block.transaction[TransactionRepository.KEY_BALANCE]} does not match calculated balance: $balanceBefore + $balanceChange ")
        }
        return // Valid
    }

    fun assertBalanceExists(block: TrustChainBlock) {
        if (!block.transaction.containsKey(TransactionRepository.KEY_BALANCE)) {
            throw MissingBalance("balance missing from transaction")
        }
    }

    private fun getUnlinkedCheckpointBlockRanges(block: TrustChainBlock, database: TrustChainStore): List<BlockRange> {
        val blockBefore = getBlockBeforeOrRaise(block, database) ?: return listOf()
        if (blockBefore.type == TransactionRepository.BLOCK_TYPE_CHECKPOINT) {
            if (database.getLinked(blockBefore) != null) {
                // Found last valid checkpoint
                return listOf()
            } else {
                // Found un-validated valid checkpoint, add to range and recurse
                return getUnlinkedCheckpointBlockRanges(blockBefore, database) + listOf(BlockRange(blockBefore.publicKey, LongRange(blockBefore.sequenceNumber.toLong(), blockBefore.sequenceNumber.toLong())))
            }
        } else {
            return getUnlinkedCheckpointBlockRanges(blockBefore, database)
        }
    }

    private fun verifyBalanceAvailable(block: TrustChainBlock, database: TrustChainStore) {
        val balance = getVerifiedBalanceForBlock(block, database) ?: throw PartialPrevious("Missing previous blocks")
        if (balance < 0) {
            // the validated balance is not enough, but it could be the case we're missing some
            // checkpoint links
            val unConfirmed = getUnlinkedCheckpointBlockRanges(block, database)
            if (unConfirmed.isNotEmpty()) { // There are some checkpoints without linked blocks
                // crawl these missing linked blocks
                throw MissingBlocks(unConfirmed)
            } else { // last checkpoint is full, spendable balance is invalid
                throw InsufficientValidatedBalance(
                    "Insufficient balance ($balance) for amount (${
                    getBalanceChangeForBlock(
                        block
                    )
                    })"
                )
            }
        }
        return // Valid
    }

    open fun validateEuroTokenProposal(block: TrustChainBlock, database: TrustChainStore) {
        verifyListedBalance(block, database)
        verifyBalanceAvailable(block, database)
    }

    open fun validateEuroTokenAcceptance(block: TrustChainBlock, database: TrustChainStore) {
        // Most validations in the checkpoints
    }

    fun validateEuroToken(block: TrustChainBlock, database: TrustChainStore) {
        if (block.isProposal) {
            validateEuroTokenProposal(block, database)
        } else {
            validateEuroTokenAcceptance(block, database)
        }
    }

    override fun validate(block: TrustChainBlock, database: TrustChainStore): ValidationResult {
        try {
            validateEuroToken(block, database)
        } catch (e: Invalid) {
            return ValidationResult.Invalid(listOf(e.TYPE, e.message ?: ""))
        } catch (e: PartialPrevious) {
            return ValidationResult.PartialPrevious
        } catch (e: MissingBlocks) {
            return ValidationResult.MissingBlocks(e.blockRanges)
        }
        return ValidationResult.Valid
    }

    abstract class ValidationResultException(message: String) : Exception(message) {
        abstract val TYPE: String
    }

    class PartialPrevious(message: String) : ValidationResultException(message) {
        override val TYPE: String = "PartialPrevious"
    }

    abstract class Invalid(message: String) : ValidationResultException(message)

    class MissingBalance(message: String) : Invalid(message) {
        override val TYPE: String = "MissingBalance"
    }

    class InsufficientBalance(message: String) : Invalid(message) {
        override val TYPE: String = "InsufficientBalance"
    }

    class InsufficientValidatedBalance(message: String) : Invalid(message) {
        override val TYPE: String = "InsufficientValidatedBalanceBalance"
    }

    class InvalidBalance(message: String) : Invalid(message) {
        override val TYPE: String = "InvalidBalance"
    }

    class MissingBlocks(val blockRanges: List<BlockRange>) : ValidationResultException(
        "MissingBlocks (" + blockRanges.joinToString(", ") + ")"
    ) {
        override val TYPE: String = "MissingBlocks"
    }
}
