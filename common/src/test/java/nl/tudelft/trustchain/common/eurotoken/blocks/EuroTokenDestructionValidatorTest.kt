package nl.tudelft.trustchain.common.eurotoken.blocks

import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

@ExperimentalUnsignedTypes
class EuroTokenDestructionValidatorTest {
    @Test
    fun test_init() {
        testBlock(blockType = TransactionRepository.BLOCK_TYPE_DESTROY)
    }

    @Test
    fun test_valid_send_id() {
        // Test Valid send after receiving
        val db = database()
        val gatewayStore = testGatewayStore()

        val g = testGateway(gatewayStore)

        val a2 = getWalletBlockWithBalance(10, db, g)

        val a3 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_DESTROY,
                transaction =
                    mapOf(
                        TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                        TransactionRepository.KEY_BALANCE to 0L,
                        TransactionRepository.KEY_PAYMENT_ID to "ID"
                    ),
                previous = a2
            )

        val result = validate(a3, db)
        assertEquals(result, ValidationResult.Valid)
    }

    @Test
    fun test_valid_send_iban() {
        // Test Valid send after receiving
        val db = database()

        val gatewayStore = testGatewayStore()
        val g = testGateway(gatewayStore)

        val a2 = getWalletBlockWithBalance(10, db, g)

        val a3 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_DESTROY,
                transaction =
                    mapOf(
                        TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                        TransactionRepository.KEY_BALANCE to 0L,
                        TransactionRepository.KEY_IBAN to "IBAN"
                    ),
                previous = a2
            )

        val result = validate(a3, db)
        assertEquals(result, ValidationResult.Valid)
    }

    @Test
    fun test_missing_amount() {
        // Test Valid send after receiving
        val db = database()

        val gatewayStore = testGatewayStore()
        val g = testGateway(gatewayStore)

        val a2 = getWalletBlockWithBalance(10, db, g)

        val a3 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_DESTROY,
                transaction =
                    mapOf(
                        TransactionRepository.KEY_BALANCE to 0L,
                        TransactionRepository.KEY_IBAN to "IBAN"
                    ),
                previous = a2
            )

        val result = validate(a3, db)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals(
            (result as ValidationResult.Invalid).errors[0],
            EuroTokenDestructionValidator.MissingAmount("").type
        )
    }

    @Test
    fun test_missing_payment_id_and_iban() {
        // Test Valid send after receiving
        val db = database()

        val gatewayStore = testGatewayStore()
        val g = testGateway(gatewayStore)

        val a2 = getWalletBlockWithBalance(10, db, g)

        val a3 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_DESTROY,
                transaction =
                    mapOf(
                        TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                        TransactionRepository.KEY_BALANCE to 0L
                    ),
                previous = a2
            )

        val result = validate(a3, db)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals(
            (result as ValidationResult.Invalid).errors[0],
            EuroTokenDestructionValidator.MissingPaymentIDorIBAN("").type
        )
    }

    @Test
    fun test_invalid_send() {
        // Test balance not deducted
        // Test Valid send after receiving
        val db = database()

        val gatewayStore = testGatewayStore()
        val g = testGateway(gatewayStore)

        val a2 = getWalletBlockWithBalance(10, db, g)

        val a3 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_DESTROY,
                transaction =
                    mapOf(
                        TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                        TransactionRepository.KEY_BALANCE to 10L,
                        TransactionRepository.KEY_IBAN to "IBAN"
                    ),
                previous = a2
            )

        val result = validate(a3, db)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals(
            (result as ValidationResult.Invalid).errors[0],
            EuroTokenBaseValidator.InvalidBalance("").type
        )
    }

    @Test
    fun test_invalid_send2() {
        // Balance is not available on transfer
        val db = database()

        val a1 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_DESTROY,
                transaction =
                    mapOf(
                        TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10),
                        TransactionRepository.KEY_BALANCE to -10L,
                        TransactionRepository.KEY_IBAN to "IBAN"
                    )
            )

        val result = validate(a1, db)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals(
            (result as ValidationResult.Invalid).errors[0],
            EuroTokenBaseValidator.InsufficientBalance("").type
        )
    }
}
