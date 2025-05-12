package nl.tudelft.trustchain.common.eurotoken.blocks

import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

@ExperimentalUnsignedTypes
class EuroTokenRollbackValidatorTest {
    @Test
    fun test_init() {
        testBlock(blockType = TransactionRepository.BLOCK_TYPE_ROLLBACK)
    }

    @Test
    fun test_valid_rollback() {
        // Test Valid rollback after receiving
        val db = database()

        // Receive money
        val b1 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_TRANSFER,
                transaction =
                mapOf(
                    TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                    TransactionRepository.KEY_BALANCE to 0L
                )
            )
        val a1 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_TRANSFER,
                transaction =
                mapOf(
                    TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                    TransactionRepository.KEY_BALANCE to 0L
                ),
                linked = b1
            )
        db.addBlock(b1)
        db.addBlock(a1)

        var result = validate(a1, db)
        assertEquals(result, ValidationResult.Valid)

        // roll back the transaction
        val a2 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_ROLLBACK,
                transaction =
                mapOf(
                    TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                    TransactionRepository.KEY_BALANCE to 0L,
                    TransactionRepository.KEY_TRANSACTION_HASH to a1.calculateHash().toHex()
                ),
                previous = a1
            )

        result = validate(a2, db)
        assertEquals(result, ValidationResult.Valid)
    }

    @Test
    fun test_valid_rollback_missing_transaction() {
        // Test Valid rollback after receiving
        val db = database()

        // Receive money
        val b1 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_TRANSFER,
                transaction =
                mapOf(
                    TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                    TransactionRepository.KEY_BALANCE to 0L
                )
            )
        val a1 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_TRANSFER,
                transaction =
                mapOf(
                    TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                    TransactionRepository.KEY_BALANCE to 0L
                ),
                linked = b1
            )
        db.addBlock(b1)
        db.addBlock(a1)

        var result = validate(a1, db)
        assertEquals(result, ValidationResult.Valid)

        // roll back the transaction
        val a2 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_ROLLBACK,
                transaction =
                mapOf(
                    TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                    TransactionRepository.KEY_BALANCE to 0L,
                    TransactionRepository.KEY_TRANSACTION_HASH to "ABCD"
                ),
                previous = a1
            )

        result = validate(a2, db)
        assertEquals(result, ValidationResult.Valid)
    }

    @Test
    fun test_missing_amount() {
        // Test Valid send after receiving
        val db = database()

        // Receive money
        val b1 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_TRANSFER,
                transaction =
                mapOf(
                    TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                    TransactionRepository.KEY_BALANCE to 0L
                )
            )
        val a1 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_TRANSFER,
                transaction =
                mapOf(
                    TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                    TransactionRepository.KEY_BALANCE to 0L
                ),
                linked = b1
            )
        db.addBlock(b1)
        db.addBlock(a1)

        var result = validate(a1, db)
        assertEquals(result, ValidationResult.Valid)

        // roll back the transaction
        val a2 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_ROLLBACK,
                transaction =
                mapOf(
                    TransactionRepository.KEY_BALANCE to 0L,
                    TransactionRepository.KEY_TRANSACTION_HASH to a1.calculateHash().toHex()
                ),
                previous = a1
            )

        result = validate(a2, db)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals(
            (result as ValidationResult.Invalid).errors[0],
            EuroTokenRollBackValidator.MissingAmount("").type
        )
    }

    @Test
    fun test_missing_hash() {
        // Test Valid rollback after receiving
        val db = database()

        // Receive money
        val b1 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_TRANSFER,
                transaction =
                mapOf(
                    TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                    TransactionRepository.KEY_BALANCE to 0L
                )
            )
        val a1 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_TRANSFER,
                transaction =
                mapOf(
                    TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                    TransactionRepository.KEY_BALANCE to 0L
                ),
                linked = b1
            )
        db.addBlock(b1)
        db.addBlock(a1)

        var result = validate(a1, db)
        assertEquals(result, ValidationResult.Valid)

        // roll back the transaction
        val a2 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_ROLLBACK,
                transaction =
                mapOf(
                    TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                    TransactionRepository.KEY_BALANCE to 0L
                ),
                previous = a1
            )

        result = validate(a2, db)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals(
            (result as ValidationResult.Invalid).errors[0],
            EuroTokenRollBackValidator.MissingTransactionHash("").type
        )
    }

    @Test
    fun test_missing_previous() {
        // Test Valid rollback after receiving
        val db = database()

        // Receive money
        val b1 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_TRANSFER,
                transaction =
                mapOf(
                    TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                    TransactionRepository.KEY_BALANCE to 0L
                )
            )
        val a1 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_TRANSFER,
                transaction =
                mapOf(
                    TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                    TransactionRepository.KEY_BALANCE to 0L
                ),
                linked = b1
            )
        db.addBlock(b1)
        // db.addBlock(A1)

        var result = validate(a1, db)
        assertEquals(result, ValidationResult.Valid)

        // roll back the transaction
        val a2 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_ROLLBACK,
                transaction =
                mapOf(
                    TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                    TransactionRepository.KEY_BALANCE to 0L,
                    TransactionRepository.KEY_TRANSACTION_HASH to a1.calculateHash().toHex()
                ),
                previous = a1
            )

        result = validate(a2, db)
        assertTrue(result is ValidationResult.PartialPrevious)
    }

    @Test
    fun test_invalid_amount() {
        // Test amount doesnt match rolled back transaction
        val db = database()

        // Receive money
        val b1 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_TRANSFER,
                transaction =
                mapOf(
                    TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                    TransactionRepository.KEY_BALANCE to 0L
                )
            )
        val a1 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_TRANSFER,
                transaction =
                mapOf(
                    TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                    TransactionRepository.KEY_BALANCE to 0L
                ),
                linked = b1
            )
        db.addBlock(b1)
        db.addBlock(a1)

        var result = validate(a1, db)
        assertEquals(result, ValidationResult.Valid)

        // roll back the transaction
        val a2 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_ROLLBACK,
                transaction =
                mapOf(
                    TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(5),
                    TransactionRepository.KEY_BALANCE to 5L,
                    TransactionRepository.KEY_TRANSACTION_HASH to a1.calculateHash().toHex()
                ),
                previous = a1
            )

        result = validate(a2, db)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals(
            (result as ValidationResult.Invalid).errors[0],
            EuroTokenRollBackValidator.InvalidTransaction("").type
        )
    }
}
