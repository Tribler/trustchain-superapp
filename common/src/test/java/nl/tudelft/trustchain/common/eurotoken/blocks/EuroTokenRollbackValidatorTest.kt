package nl.tudelft.trustchain.common.eurotoken.blocks
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import org.junit.Test
import org.junit.Assert.*
import java.math.BigInteger

@ExperimentalUnsignedTypes
class EuroTokenRollbackValidatorTest {

    @Test
    fun test_init() {
        TestBlock(block_type = TransactionRepository.BLOCK_TYPE_ROLLBACK)
    }

    @Test
    fun test_valid_rollback() {
        // Test Valid rollback after receiving
        val db = Database()

        // Receive money
        val B1 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_TRANSFER,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                TransactionRepository.KEY_BALANCE to 0L
            )
        )
        val A1 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_TRANSFER,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                TransactionRepository.KEY_BALANCE to 0L
            ),
            linked = B1
        )
        db.addBlock(B1)
        db.addBlock(A1)

        var result = validate(A1, db)
        assertEquals(result, ValidationResult.Valid)

        // roll back the transaction
        val A2 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_ROLLBACK,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                TransactionRepository.KEY_BALANCE to 0L,
                TransactionRepository.KEY_TRANSACTION_HASH to A1.calculateHash().toHex()
            ),
            previous = A1)

        result = validate(A2, db)
        assertEquals(result, ValidationResult.Valid)
    }

    @Test
    fun test_valid_rollback_missing_transaction() {
        // Test Valid rollback after receiving
        val db = Database()

        // Receive money
        val B1 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_TRANSFER,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                TransactionRepository.KEY_BALANCE to 0L
            )
        )
        val A1 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_TRANSFER,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                TransactionRepository.KEY_BALANCE to 0L
            ),
            linked = B1
        )
        db.addBlock(B1)
        db.addBlock(A1)

        var result = validate(A1, db)
        assertEquals(result, ValidationResult.Valid)

        // roll back the transaction
        val A2 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_ROLLBACK,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                TransactionRepository.KEY_BALANCE to 0L,
                TransactionRepository.KEY_TRANSACTION_HASH to "ABCD"
            ),
            previous = A1)

        result = validate(A2, db)
        assertEquals(result, ValidationResult.Valid)
    }

    @Test
    fun test_missing_amount() {
        // Test Valid send after receiving
        val db = Database()

        // Receive money
        val B1 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_TRANSFER,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                TransactionRepository.KEY_BALANCE to 0L
            )
        )
        val A1 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_TRANSFER,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                TransactionRepository.KEY_BALANCE to 0L
            ),
            linked = B1
        )
        db.addBlock(B1)
        db.addBlock(A1)

        var result = validate(A1, db)
        assertEquals(result, ValidationResult.Valid)

        // roll back the transaction
        val A2 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_ROLLBACK,
            transaction = mapOf(
                TransactionRepository.KEY_BALANCE to 0L,
                TransactionRepository.KEY_TRANSACTION_HASH to A1.calculateHash().toHex()
            ),
            previous = A1)

        result = validate(A2, db)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals((result as ValidationResult.Invalid).errors[0], EuroTokenRollBackValidator.MissingAmount("").TYPE)
    }

    @Test
    fun test_missing_hash() {
        // Test Valid rollback after receiving
        val db = Database()

        // Receive money
        val B1 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_TRANSFER,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                TransactionRepository.KEY_BALANCE to 0L
            )
        )
        val A1 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_TRANSFER,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                TransactionRepository.KEY_BALANCE to 0L
            ),
            linked = B1
        )
        db.addBlock(B1)
        db.addBlock(A1)

        var result = validate(A1, db)
        assertEquals(result, ValidationResult.Valid)

        // roll back the transaction
        val A2 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_ROLLBACK,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                TransactionRepository.KEY_BALANCE to 0L
            ),
            previous = A1)

        result = validate(A2, db)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals((result as ValidationResult.Invalid).errors[0], EuroTokenRollBackValidator.MissingTransactionHash("").TYPE)
    }

    @Test
    fun test_missing_previous() {
        // Test Valid rollback after receiving
        val db = Database()

        // Receive money
        val B1 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_TRANSFER,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                TransactionRepository.KEY_BALANCE to 0L
            )
        )
        val A1 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_TRANSFER,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                TransactionRepository.KEY_BALANCE to 0L
            ),
            linked = B1
        )
        db.addBlock(B1)
        // db.addBlock(A1)

        var result = validate(A1, db)
        assertEquals(result, ValidationResult.Valid)

        // roll back the transaction
        val A2 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_ROLLBACK,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                TransactionRepository.KEY_BALANCE to 0L,
                TransactionRepository.KEY_TRANSACTION_HASH to A1.calculateHash().toHex()
            ),
            previous = A1)

        result = validate(A2, db)
        assertTrue(result is ValidationResult.PartialPrevious)
    }

    @Test
    fun test_invalid_amount() {
        // Test amount doesnt match rolled back transaction
        val db = Database()

        // Receive money
        val B1 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_TRANSFER,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                TransactionRepository.KEY_BALANCE to 0L
            )
        )
        val A1 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_TRANSFER,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                TransactionRepository.KEY_BALANCE to 0L
            ),
            linked = B1
        )
        db.addBlock(B1)
        db.addBlock(A1)

        var result = validate(A1, db)
        assertEquals(result, ValidationResult.Valid)

        // roll back the transaction
        val A2 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_ROLLBACK,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(5),
                TransactionRepository.KEY_BALANCE to 5L,
                TransactionRepository.KEY_TRANSACTION_HASH to A1.calculateHash().toHex()
            ),
            previous = A1)

        result = validate(A2, db)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals((result as ValidationResult.Invalid).errors[0], EuroTokenRollBackValidator.InvalidTransaction("").TYPE)
    }
}
