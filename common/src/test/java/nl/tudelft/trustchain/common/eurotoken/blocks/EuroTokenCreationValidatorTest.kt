package nl.tudelft.trustchain.common.eurotoken.blocks

import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import org.junit.Assert.*
import org.junit.Test

@ExperimentalUnsignedTypes
class EuroTokenCreationValidatorTest {

    @Test
    fun test_init() {
        TestBlock(block_type = TransactionRepository.BLOCK_TYPE_CREATE)
    }

    @Test
    fun test_missing_amount() {
        val db = Database()

        val block = TestBlock(block_type = TransactionRepository.BLOCK_TYPE_CREATE, transaction = mapOf())

        val result = validate(block, db)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals((result as ValidationResult.Invalid).errors[0], EuroTokenCreationValidator.MissingAmount("").TYPE)
    }

    @Test
    fun test_valid_creation() {
        // Test Valid send after receiving
        val db = Database()

        val gatewayStore = TestGatewayStore()
        val G = TestGateway(gatewayStore)

        val A = TestWallet()

        val G1 = TestBlock(
            key = G,
            block_type = TransactionRepository.BLOCK_TYPE_CREATE,
            transaction = mapOf(
                TransactionRepository.KEY_BALANCE to 10L
            ),
            links = A.pub()
        )
        db.addBlock(G1)
        val A1 = TestBlock(
            key = A,
            block_type = TransactionRepository.BLOCK_TYPE_CREATE,
            transaction = mapOf(
                TransactionRepository.KEY_BALANCE to 10L
            ),
            linked = G1
        )
        val result = validate(A1, db)
        assertEquals(result, ValidationResult.Valid)

        db.addBlock(A1)
    }
}
