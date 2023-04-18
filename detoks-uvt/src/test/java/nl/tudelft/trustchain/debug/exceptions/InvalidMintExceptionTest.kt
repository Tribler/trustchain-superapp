package nl.tudelft.trustchain.debug.exceptions

import nl.tudelft.trustchain.detoks.exception.InvalidMintException
import org.junit.Assert
import org.junit.Test

/**
 * Test suite for the []InvalidMintException]
 */
class InvalidMintExceptionTest {

    @Test
    fun exceptionTest() {
        val exceptionMessage = "This mint is invalid"

        try {
            throw InvalidMintException(exceptionMessage)
        }
        catch (e: InvalidMintException) {
            Assert.assertEquals(exceptionMessage, e.message)
        }
    }
}
