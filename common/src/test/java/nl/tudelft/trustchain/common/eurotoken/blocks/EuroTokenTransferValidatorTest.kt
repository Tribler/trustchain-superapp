package nl.tudelft.trustchain.common.eurotoken.blocks

import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

@ExperimentalUnsignedTypes
class EuroTokenTransferValidatorTest {

    @Test
    fun test_init() {
        TestBlock(block_type = TransactionRepository.BLOCK_TYPE_TRANSFER)
    }

    @Test
    fun test_valid_send() {
        // Test Valid send after receiving
        val db = Database()

        val gatewayStore = TestGatewayStore()
        val G = TestGateway(gatewayStore)

        val A2 = getWalletBlockWithBalance(10, db, G)

        val A3 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_TRANSFER,
            transaction = mapOf(
                TransactionRepository.KEY_BALANCE to 0L,
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10)
            ),
            previous = A2
        )

        val result = validate(A3, db)
        assertEquals(result, ValidationResult.Valid)
    }

    @Test
    fun test_missing_amount() {
        // Test missing amount
        val db = Database()

        val gatewayStore = TestGatewayStore()
        val G = TestGateway(gatewayStore)

        val A2 = getWalletBlockWithBalance(10, db, G)

        val A3 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_TRANSFER,
            transaction = mapOf(
                TransactionRepository.KEY_BALANCE to 0L
            ),
            previous = A2
        )

        val result = validate(A3, db)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals((result as ValidationResult.Invalid).errors[0], EuroTokenTransferValidator.MissingAmount("").TYPE)
    }

    @Test
    fun test_invalid_send() {
        // Balance is not deducted on transfer
        val db = Database()

        val gatewayStore = TestGatewayStore()
        val G = TestGateway(gatewayStore)

        val A2 = getWalletBlockWithBalance(10, db, G)

        val A3 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_TRANSFER,
            transaction = mapOf(
                TransactionRepository.KEY_BALANCE to 10L,
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10)
            ),
            previous = A2
        )

        val result = validate(A3, db)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals((result as ValidationResult.Invalid).errors[0], EuroTokenBaseValidator.InvalidBalance("").TYPE)
    }

    @Test
    fun test_invalid_send2() {
        // Balance is not available on transfer
        val db = Database()
        val A1 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_TRANSFER,
            transaction = mapOf(
                TransactionRepository.KEY_BALANCE to -10L,
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10)
            )
        )
        val result = validate(A1, db)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals((result as ValidationResult.Invalid).errors[0], EuroTokenBaseValidator.InsufficientBalance("").TYPE)
    }
}
