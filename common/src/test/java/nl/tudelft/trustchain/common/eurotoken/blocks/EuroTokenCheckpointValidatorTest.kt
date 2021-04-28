package nl.tudelft.trustchain.common.eurotoken.blocks

import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

@ExperimentalUnsignedTypes
class EuroTokenCheckpointValidatorTest {
    @Test
    fun test_init() {
        TestBlock(block_type = TransactionRepository.BLOCK_TYPE_CHECKPOINT)
    }

    @Test
    fun test_missing_checkpoint_links() {
        // Test validating a block that points towards a previous block
        val db = Database()

        val gatewayStore = TestGatewayStore()
        val G = TestGateway(gatewayStore)

        val A = TestWallet()

        val G1 = TestBlock(
            key = G,
            block_type = TransactionRepository.BLOCK_TYPE_CREATE,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10)
            ),
            links = A.pub()
        )

        val A1 = TestBlock(
            key = A,
            block_type = TransactionRepository.BLOCK_TYPE_TRANSFER,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10)
            ),
            linked = G1
        )
        db.addBlock(G1)
        db.addBlock(A1)

        val A2 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_CHECKPOINT,
            transaction = mapOf(
                TransactionRepository.KEY_BALANCE to 10L
            ),
            previous = A1,
            links = G.pub()
        )

        var result = validate(A2, db)
        assertEquals(result, ValidationResult.Valid)
        db.addBlock(A2)

        val G2 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_CREATE,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10)
            ),
            previous = G1,
            links = A.pub()
        )
        db.addBlock(G2)

        val A3 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_CREATE,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10)
            ),
            previous = A2,
            linked = G2
        )

        result = validate(A3, db)
        assertEquals(result, ValidationResult.Valid)
        db.addBlock(A3)

        val A4 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_CHECKPOINT,
            transaction = mapOf(
                TransactionRepository.KEY_BALANCE to 20L
            ),
            previous = A3,
            links = G.pub()
        )

        result = validate(A4, db)
        assertEquals(result, ValidationResult.Valid)
        db.addBlock(A4)

        val A5 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_TRANSFER,
            transaction = mapOf(
                TransactionRepository.KEY_BALANCE to 0L,
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(20)
            ),
            previous = A4
        )

        result = validate(A5, db)
        assertTrue(result is ValidationResult.MissingBlocks)
        assertEquals((result as ValidationResult.MissingBlocks).blockRanges.size, 2)

        val G3 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_CHECKPOINT,
            transaction = mapOf(
                TransactionRepository.KEY_BALANCE to 10L
            ),
            previous = G2,
            linked = A2
        )
        db.addBlock(G3)

        result = validate(A5, db)
        println(result)
        assertTrue(result is ValidationResult.MissingBlocks)
        assertEquals((result as ValidationResult.MissingBlocks).blockRanges.size, 1)
    }
}
