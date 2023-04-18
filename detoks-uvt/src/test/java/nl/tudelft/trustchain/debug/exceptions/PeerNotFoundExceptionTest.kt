package nl.tudelft.trustchain.debug.exceptions

import nl.tudelft.trustchain.detoks.exception.PeerNotFoundException
import org.junit.Assert
import org.junit.Test

/**
 * Test suite for the [PeerNotFoundException]
 */
class PeerNotFoundExceptionTest {

    @Test
    fun exceptionTest() {
        val exceptionMessage = "Could not find a peer!"

        try {
            throw PeerNotFoundException(exceptionMessage)
        }
        catch (e: PeerNotFoundException) {
            Assert.assertEquals(exceptionMessage, e.message)
        }
    }
}
