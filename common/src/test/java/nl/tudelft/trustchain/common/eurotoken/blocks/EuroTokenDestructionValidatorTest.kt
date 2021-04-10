package nl.tudelft.trustchain.common.eurotoken.blocks

import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository

import org.junit.Test
import org.junit.Assert.*
import java.math.BigInteger

@ExperimentalUnsignedTypes
class EuroTokenDestructionValidatorTest {

    @Test
    fun test_init() {
        TestBlock(block_type = TransactionRepository.BLOCK_TYPE_DESTROY)
    }

    @Test
    fun test_valid_send_id() {
        // Test Valid send after receiving
        val db = Database()
        val gatewayStore = TestGatewayStore()

        val G = TestGateway(gatewayStore)

        val A2 = getWalletBlockWithBalance(10, db, G)

        val A3 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_DESTROY,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                TransactionRepository.KEY_BALANCE to 0L,
                TransactionRepository.KEY_PAYMENT_ID to "ID"
            ),
            previous = A2)

        val result = validate(A3, db)
        assertEquals(result, ValidationResult.Valid)
    }

    @Test
    fun test_valid_send_iban() {
        // Test Valid send after receiving
        val db = Database()

        val gatewayStore = TestGatewayStore()
        val G = TestGateway(gatewayStore)

        val A2 = getWalletBlockWithBalance(10, db, G)

        val A3 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_DESTROY,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                TransactionRepository.KEY_BALANCE to 0L,
                TransactionRepository.KEY_IBAN to "IBAN"
            ),
            previous = A2)

        val result = validate(A3, db)
        assertEquals(result, ValidationResult.Valid)
    }

    @Test
    fun test_missing_amount() {
        // Test Valid send after receiving
        val db = Database()

        val gatewayStore = TestGatewayStore()
        val G = TestGateway(gatewayStore)

        val A2 = getWalletBlockWithBalance(10, db, G)

        val A3 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_DESTROY,
            transaction = mapOf(
                TransactionRepository.KEY_BALANCE to 0L,
                TransactionRepository.KEY_IBAN to "IBAN"
            ),
            previous = A2)

        val result = validate(A3, db)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals((result as ValidationResult.Invalid).errors[0], EuroTokenDestructionValidator.MissingAmount("").TYPE)
    }

    @Test
    fun test_missing_payment_id_and_iban() {
        // Test Valid send after receiving
        val db = Database()

        val gatewayStore = TestGatewayStore()
        val G = TestGateway(gatewayStore)

        val A2 = getWalletBlockWithBalance(10, db, G)

        val A3 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_DESTROY,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                TransactionRepository.KEY_BALANCE to 0L
            ),
            previous = A2)

        val result = validate(A3, db)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals((result as ValidationResult.Invalid).errors[0], EuroTokenDestructionValidator.MissingPaymentIDorIBAN("").TYPE)
    }

    @Test
    fun test_invalid_send() {
        // Test balance not deducted
        // Test Valid send after receiving
        val db = Database()

        val gatewayStore = TestGatewayStore()
        val G = TestGateway(gatewayStore)

        val A2 = getWalletBlockWithBalance(10, db, G)

        val A3 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_DESTROY,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                TransactionRepository.KEY_BALANCE to 10L,
                TransactionRepository.KEY_IBAN to "IBAN"
            ),
            previous = A2)

        val result = validate(A3, db)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals((result as ValidationResult.Invalid).errors[0], EuroTokenBaseValidator.InvalidBalance("").TYPE)
    }

    @Test
    fun test_invalid_send2() {
        // Balance is not available on transfer
        val db = Database()

        val A1 = TestBlock(
            block_type = TransactionRepository.BLOCK_TYPE_DESTROY,
            transaction = mapOf(
                TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                TransactionRepository.KEY_BALANCE to -10L,
                TransactionRepository.KEY_IBAN to "IBAN"
            )
        )

        val result = validate(A1, db)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals((result as ValidationResult.Invalid).errors[0], EuroTokenBaseValidator.InsufficientBalance("").TYPE)
    }
}
