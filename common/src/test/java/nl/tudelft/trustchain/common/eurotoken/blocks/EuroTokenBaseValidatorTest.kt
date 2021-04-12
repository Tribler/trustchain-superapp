package nl.tudelft.trustchain.common.eurotoken.blocks

import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.eurotoken.getBalanceForBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

@ExperimentalUnsignedTypes
class EuroTokenBaseValidatorTest {

    @Test
    fun validateEuroTokenBlockCreate() {
        TestBlock(block_type = TransactionRepository.BLOCK_TYPE_CHECKPOINT)
    }

    @Test
    fun test_valid_balance_genesis() {
        // Test validating a genesis block with 0 balance
        val db = Database()
        val block = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_CHECKPOINT,
            transaction = mapOf(TransactionRepository.KEY_BALANCE to 0L)
        )
        val result = validate(block, db)
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun test_invalid_balance_genesis() {
        // Test validating a genesis block with an unearned balance
        val db = Database()
        val block = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_TRANSFER,
            transaction = mapOf(
                TransactionRepository.KEY_BALANCE to 1L,
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(5)
            )
        )
        val result = validate(block, db)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals((result as ValidationResult.Invalid).errors[0], EuroTokenBaseValidator.InvalidBalance("").TYPE)
    }

    @Test
    fun test_missing_balance() {
        // Test validating a genesis block without a balance
        val db = Database()
        val block = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_CHECKPOINT,
            transaction = mapOf()
        )
        val result = validate(block, db)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals((result as ValidationResult.Invalid).errors[0], EuroTokenBaseValidator.MissingBalance("").TYPE)
    }

    @Test
    fun test_missing_validated_balance() {
        val db = Database()
        val gatewayStore = TestGatewayStore()
        val A = TestWallet()
        val G = TestGateway(gatewayStore)

        val G1 = TestBlock(
            key = G,
            block_type = TransactionRepository.BLOCK_TYPE_CREATE,
            transaction = mapOf(TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10)),
            links = A.pub()
        )

        var result = validate(G1, db)
        assertEquals(ValidationResult.Valid, result)
        db.addBlock(G1)

        val A1 = TestBlock(
            key = A,
            block_type = TransactionRepository.BLOCK_TYPE_CREATE,
            transaction = mapOf(TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10)),
            linked = G1
        )

        result = validate(A1, db)
        assertEquals(ValidationResult.Valid, result)
        db.addBlock(A1)

        val A2 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_TRANSFER,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(5),
                TransactionRepository.KEY_BALANCE to 5L
            ),
            previous = A1
        )
        result = validate(A2, db)
        assertEquals((result as ValidationResult.Invalid).errors[0], EuroTokenBaseValidator.InsufficientValidatedBalance("").TYPE)
    }

    @Test
    fun test_double_validate() {
        // Test validating a block that points towards a previous block
        val db = Database()

        val prev = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_CHECKPOINT,
            transaction = mapOf(TransactionRepository.KEY_BALANCE to 0L)
        )
        var result = validate(prev, db)
        assertEquals(result, ValidationResult.Valid)
        db.addBlock(prev)

        val block = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_CHECKPOINT,
            transaction = mapOf(TransactionRepository.KEY_BALANCE to 0L),
            previous = prev
        )
        result = validate(block, db)
        assertEquals(result, ValidationResult.Valid)
    }

    @Test
    fun test_double_validate_skip_non_eurotoken() {
        // Test validating a block that points towards a previous block with a non eurotoken block in between
        val db = Database()
        val gatewayStore = TestGatewayStore()

        val G = TestGateway(gatewayStore)

        val A1 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_CHECKPOINT,
            transaction = mapOf(TransactionRepository.KEY_BALANCE to 0L),
            links = G.pub()
        )
        var result = validate(A1, db)
        assertEquals(result, ValidationResult.Valid)
        db.addBlock(A1)

        val A2 = TestBlock(
            block_type = "NonEurotoken",
            transaction = mapOf(),
            previous = A1
        )
        db.addBlock(A2)

        val A3 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_CHECKPOINT,
            transaction = mapOf(TransactionRepository.KEY_BALANCE to 0L),
            previous = A2,
            links = G.pub()
        )
        result = validate(A3, db)
        assertEquals(result, ValidationResult.Valid)
    }

    @Test
    fun test_missing_previous() {
        // Test validating a block that points towards a missing block
        val db = Database()

        val gatewayStore = TestGatewayStore()

        val G = TestGateway(gatewayStore)

        val A1 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_CHECKPOINT,
            transaction = mapOf(TransactionRepository.KEY_BALANCE to 0L),
            links = G.pub()
        )
        var result = validate(A1, db)
        assertEquals(result, ValidationResult.Valid)
        // db.addBlock(A1) MISSING!!!

        val A2 = TestBlock(
            block_type = "NonEurotoken",
            transaction = mapOf(),
            previous = A1
        )
        db.addBlock(A2)

        val A3 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_TRANSFER,
            transaction = mapOf(
                TransactionRepository.KEY_BALANCE to 0L,
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(5)
            ),
            previous = A2,
            links = G.pub()
        )
        result = validate(A3, db)
        assertEquals(result, ValidationResult.PartialPrevious)
    }

    @Test
    fun test_get_balance_receive_block_with_crawl() {
        val db = Database()
        val gatewayStore = TestGatewayStore()

        val G = TestGateway(gatewayStore)

        val A = TestWallet()

        val A1 = TestBlock(
            key = A,
            block_type = TransactionRepository.BLOCK_TYPE_CHECKPOINT,
            transaction = mapOf(TransactionRepository.KEY_BALANCE to 0L)
        )
        var result = validate(A1, db)
        assertEquals(result, ValidationResult.Valid)
        db.addBlock(A1)

        val B2 = getWalletBlockWithBalance(10, db, G)

        val B3 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_TRANSFER,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(5),
                TransactionRepository.KEY_BALANCE to 5L
            ),
            previous = B2,
            links = A.pub()
        )
        result = validate(B3, db)
        assertEquals(result, ValidationResult.Valid)
        db.addBlock(B3)

        val A2 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_TRANSFER,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(5),
                TransactionRepository.KEY_BALANCE to 5L
            ),
            previous = A1,
            linked = B3
        )
        val balance = getBalanceForBlock(A2, db)
        assertEquals(balance, 5L)
    }
}
