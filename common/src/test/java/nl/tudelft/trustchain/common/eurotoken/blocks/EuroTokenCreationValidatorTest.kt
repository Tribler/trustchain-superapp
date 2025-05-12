package nl.tudelft.trustchain.common.eurotoken.blocks

import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@ExperimentalUnsignedTypes
class EuroTokenCreationValidatorTest {
    @Test
    fun test_init() {
        testBlock(blockType = TransactionRepository.BLOCK_TYPE_CREATE)
    }

    @Test
    fun test_missing_amount() {
        val db = database()

        val block = testBlock(blockType = TransactionRepository.BLOCK_TYPE_CREATE, transaction = mapOf())

        val result = validate(block, db)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals((result as ValidationResult.Invalid).errors[0], EuroTokenCreationValidator.MissingAmount("").type)
    }

    @Test
    fun test_valid_creation() {
        // Test Valid send after receiving
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
                    TransactionRepository.KEY_BALANCE to 10L
                ),
                links = a.pub()
            )
        db.addBlock(g1)
        val a1 =
            testBlock(
                key = a,
                blockType = TransactionRepository.BLOCK_TYPE_CREATE,
                transaction =
                mapOf(
                    TransactionRepository.KEY_BALANCE to 10L
                ),
                linked = g1
            )
        val result = validate(a1, db)
        assertEquals(result, ValidationResult.Valid)

        db.addBlock(a1)
    }
}
