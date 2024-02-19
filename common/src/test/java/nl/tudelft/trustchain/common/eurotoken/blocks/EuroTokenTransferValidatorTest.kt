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
        testBlock(blockType = TransactionRepository.BLOCK_TYPE_TRANSFER)
    }

    @Test
    fun test_valid_send() {
        // Test Valid send after receiving
        val db = database()

        val gatewayStore = testGatewayStore()
        val g = testGateway(gatewayStore)

        val a2 = getWalletBlockWithBalance(10, db, g)

        val a3 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_TRANSFER,
                transaction =
                    mapOf(
                        TransactionRepository.KEY_BALANCE to 0L,
                        TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10)
                    ),
                previous = a2
            )

        val result = validate(a3, db)
        assertEquals(result, ValidationResult.Valid)
    }

    @Test
    fun test_missing_amount() {
        // Test missing amount
        val db = database()

        val gatewayStore = testGatewayStore()
        val g = testGateway(gatewayStore)

        val a2 = getWalletBlockWithBalance(10, db, g)

        val a3 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_TRANSFER,
                transaction =
                    mapOf(
                        TransactionRepository.KEY_BALANCE to 0L
                    ),
                previous = a2
            )

        val result = validate(a3, db)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals(
            (result as ValidationResult.Invalid).errors[0],
            EuroTokenTransferValidator.MissingAmount("").type
        )
    }

    @Test
    fun test_invalid_send() {
        // Balance is not deducted on transfer
        val db = database()

        val gatewayStore = testGatewayStore()
        val gateway = testGateway(gatewayStore)

        val a2 = getWalletBlockWithBalance(10, db, gateway)

        val a3 =
            testBlock(
                blockType = TransactionRepository.BLOCK_TYPE_TRANSFER,
                transaction =
                    mapOf(
                        TransactionRepository.KEY_BALANCE to 10L,
                        TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10)
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
                blockType = TransactionRepository.BLOCK_TYPE_TRANSFER,
                transaction =
                    mapOf(
                        TransactionRepository.KEY_BALANCE to -10L,
                        TransactionRepository.KEY_AMOUNT to BigInteger.valueOf(10)
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
