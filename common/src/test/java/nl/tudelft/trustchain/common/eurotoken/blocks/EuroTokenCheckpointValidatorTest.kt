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
        testBlock(blockType = TransactionRepository.BLOCK_TYPE_CHECKPOINT)
    }

    @Test
    fun test_missing_checkpoint_links() {
        // Test validating a block that points towards a previous block
        val db = database()

        val gatewayStore = testGatewayStore()
        val g = testGateway(gatewayStore)

        val a = testWallet()

        val g1 =
            testBlock(
                key = g,
                blockType = TransactionRepository.BLOCK_TYPE_CREATE,
                transaction =
                    mapOf(
                        TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10)
                    ),
                links = a.pub()
            )

        val a1 =
            testBlock(
                key = a,
                blockType = TransactionRepository.BLOCK_TYPE_TRANSFER,
                transaction =
                    mapOf(
                        TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10)
                    ),
                linked = g1
            )
        db.addBlock(g1)
        db.addBlock(a1)

        val a2 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_CHECKPOINT,
                transaction =
                    mapOf(
                        TransactionRepository.KEY_BALANCE to 10L
                    ),
                previous = a1,
                links = g.pub()
            )

        var result = validate(a2, db)
        assertEquals(result, ValidationResult.Valid)
        db.addBlock(a2)

        val g2 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_CREATE,
                transaction =
                    mapOf(
                        TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10)
                    ),
                previous = g1,
                links = a.pub()
            )
        db.addBlock(g2)

        val a3 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_CREATE,
                transaction =
                    mapOf(
                        TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10)
                    ),
                previous = a2,
                linked = g2
            )

        result = validate(a3, db)
        assertEquals(result, ValidationResult.Valid)
        db.addBlock(a3)

        val a4 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_CHECKPOINT,
                transaction =
                    mapOf(
                        TransactionRepository.KEY_BALANCE to 20L
                    ),
                previous = a3,
                links = g.pub()
            )

        result = validate(a4, db)
        assertEquals(result, ValidationResult.Valid)
        db.addBlock(a4)

        val a5 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_TRANSFER,
                transaction =
                    mapOf(
                        TransactionRepository.KEY_BALANCE to 0L,
                        TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(20)
                    ),
                previous = a4
            )

        result = validate(a5, db)
        assertTrue(result is ValidationResult.MissingBlocks)
        assertEquals((result as ValidationResult.MissingBlocks).blockRanges.size, 2)

        val g3 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_CHECKPOINT,
                transaction =
                    mapOf(
                        TransactionRepository.KEY_BALANCE to 10L
                    ),
                previous = g2,
                linked = a2
            )
        db.addBlock(g3)

        result = validate(a5, db)
        println(result)
        assertTrue(result is ValidationResult.MissingBlocks)
        assertEquals((result as ValidationResult.MissingBlocks).blockRanges.size, 1)
    }
}
