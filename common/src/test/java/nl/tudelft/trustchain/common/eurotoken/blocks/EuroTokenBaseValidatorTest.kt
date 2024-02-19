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
        testBlock(blockType = TransactionRepository.BLOCK_TYPE_CHECKPOINT)
    }

    @Test
    fun test_valid_balance_genesis() {
        // Test validating a genesis block with 0 balance
        val db = database()
        val block =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_CHECKPOINT,
                transaction = mapOf(TransactionRepository.KEY_BALANCE to 0L)
            )
        val result = validate(block, db)
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun test_invalid_balance_genesis() {
        // Test validating a genesis block with an unearned balance
        val db = database()
        val block =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_TRANSFER,
                transaction =
                    mapOf(
                        TransactionRepository.KEY_BALANCE to 1L,
                        TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(5)
                    )
            )
        val result = validate(block, db)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals(
            (result as ValidationResult.Invalid).errors[0],
            EuroTokenBaseValidator.InvalidBalance("").type
        )
    }

    @Test
    fun test_missing_balance() {
        // Test validating a genesis block without a balance
        val db = database()
        val block =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_CHECKPOINT,
                transaction = mapOf()
            )
        val result = validate(block, db)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals(
            (result as ValidationResult.Invalid).errors[0],
            EuroTokenBaseValidator.MissingBalance("").type
        )
    }

    @Test
    fun test_missing_validated_balance() {
        val db = database()
        val gatewayStore = testGatewayStore()
        val a = testWallet()
        val g = testGateway(gatewayStore)

        val g1 =
            testBlock(
                key = g,
                blockType = TransactionRepository.BLOCK_TYPE_CREATE,
                transaction = mapOf(TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10)),
                links = a.pub()
            )

        var result = validate(g1, db)
        assertEquals(ValidationResult.Valid, result)
        db.addBlock(g1)

        val a1 =
            testBlock(
                key = a,
                blockType = TransactionRepository.BLOCK_TYPE_CREATE,
                transaction = mapOf(TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10)),
                linked = g1
            )

        result = validate(a1, db)
        assertEquals(ValidationResult.Valid, result)
        db.addBlock(a1)

        val a2 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_TRANSFER,
                transaction =
                    mapOf(
                        TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(5),
                        TransactionRepository.KEY_BALANCE to 5L
                    ),
                previous = a1
            )
        result = validate(a2, db)
        assertEquals(
            (result as ValidationResult.Invalid).errors[0],
            EuroTokenBaseValidator.InsufficientValidatedBalance("").type
        )
    }

    @Test
    fun test_double_validate() {
        // Test validating a block that points towards a previous block
        val db = database()

        val prev =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_CHECKPOINT,
                transaction = mapOf(TransactionRepository.KEY_BALANCE to 0L)
            )
        var result = validate(prev, db)
        assertEquals(result, ValidationResult.Valid)
        db.addBlock(prev)

        val block =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_CHECKPOINT,
                transaction = mapOf(TransactionRepository.KEY_BALANCE to 0L),
                previous = prev
            )
        result = validate(block, db)
        assertEquals(result, ValidationResult.Valid)
    }

    @Test
    fun test_double_validate_skip_non_eurotoken() {
        // Test validating a block that points towards a previous block with a non eurotoken block in between
        val db = database()
        val gatewayStore = testGatewayStore()

        val g = testGateway(gatewayStore)

        val a1 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_CHECKPOINT,
                transaction = mapOf(TransactionRepository.KEY_BALANCE to 0L),
                links = g.pub()
            )
        var result = validate(a1, db)
        assertEquals(result, ValidationResult.Valid)
        db.addBlock(a1)

        val a2 =
            testBlock(
                blockType = "NonEurotoken",
                transaction = mapOf(),
                previous = a1
            )
        db.addBlock(a2)

        val a3 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_CHECKPOINT,
                transaction = mapOf(TransactionRepository.KEY_BALANCE to 0L),
                previous = a2,
                links = g.pub()
            )
        result = validate(a3, db)
        assertEquals(result, ValidationResult.Valid)
    }

    @Test
    fun test_missing_previous() {
        // Test validating a block that points towards a missing block
        val db = database()

        val gatewayStore = testGatewayStore()

        val g = testGateway(gatewayStore)

        val a1 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_CHECKPOINT,
                transaction = mapOf(TransactionRepository.KEY_BALANCE to 0L),
                links = g.pub()
            )
        var result = validate(a1, db)
        assertEquals(result, ValidationResult.Valid)
        // db.addBlock(A1) MISSING!!!

        val a2 =
            testBlock(
                blockType = "NonEurotoken",
                transaction = mapOf(),
                previous = a1
            )
        db.addBlock(a2)

        val a3 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_TRANSFER,
                transaction =
                    mapOf(
                        TransactionRepository.KEY_BALANCE to 0L,
                        TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(5)
                    ),
                previous = a2,
                links = g.pub()
            )
        result = validate(a3, db)
        assertEquals(result, ValidationResult.PartialPrevious)
    }

    @Test
    fun test_get_balance_receive_block_with_crawl() {
        val db = database()
        val gatewayStore = testGatewayStore()

        val g = testGateway(gatewayStore)

        val a = testWallet()

        val a1 =
            testBlock(
                key = a,
                blockType = TransactionRepository.BLOCK_TYPE_CHECKPOINT,
                transaction = mapOf(TransactionRepository.KEY_BALANCE to 0L)
            )
        var result = validate(a1, db)
        assertEquals(result, ValidationResult.Valid)
        db.addBlock(a1)

        val b2 = getWalletBlockWithBalance(10, db, g)

        val b3 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_TRANSFER,
                transaction =
                    mapOf(
                        TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(5),
                        TransactionRepository.KEY_BALANCE to 5L
                    ),
                previous = b2,
                links = a.pub()
            )
        result = validate(b3, db)
        assertEquals(result, ValidationResult.Valid)
        db.addBlock(b3)

        val a2 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_TRANSFER,
                transaction =
                    mapOf(
                        TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(5),
                        TransactionRepository.KEY_BALANCE to 5L
                    ),
                previous = a1,
                linked = b3
            )
        val balance = getBalanceForBlock(a2, db)
        assertEquals(balance, 5L)
    }
}
