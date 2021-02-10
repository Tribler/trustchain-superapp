package nl.tudelft.trustchain.trader.validators

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.trustchain.trader.util.getAmount
import nl.tudelft.trustchain.trader.util.getBalance
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@kotlin.ExperimentalUnsignedTypes
class DDValidatorTest {
    private val validator = DDValidator()

    private val block = mockk<TrustChainBlock>(relaxed = true)
    private val database = mockk<TrustChainStore>()

    @Before
    fun init() {
        mockkStatic("nl.tudelft.trustchain.trader.util.ValidatorUtilKt")
    }

    @Test
    fun offline_isValid() {
        every { block.transaction } returns mapOf("offline" to true)
        val valid = validator.validate(block, database)
        assertEquals(valid, ValidationResult.Valid)
    }

    @Test
    fun selfSigned_isValid() {
        every { block.isSelfSigned } returns true
        val valid = validator.validate(block, database)
        assertEquals(valid, ValidationResult.Valid)
    }

    @Test
    fun proposalBlockWithTooLittleCurrency_isInvalid() {
        every { block.isProposal } returns true
        every { getBalance(any(), any(), any()) } returns 5f
        every { getAmount(any()) } returns 10f

        val valid = validator.validate(block, database)

        assertEquals(valid, ValidationResult.Invalid(listOf("")))
    }

    @Test
    fun proposalBlockWithEnoughCurrency_isValid() {
        every { block.isProposal } returns true
        every { getBalance(any(), any(), any()) } returns 10f
        every { getAmount(any()) } returns 5f

        val valid = validator.validate(block, database)

        assertEquals(valid, ValidationResult.Valid)
    }
}
