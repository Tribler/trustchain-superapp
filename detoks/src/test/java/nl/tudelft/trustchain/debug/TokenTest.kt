package nl.tudelft.trustchain.debug

import nl.tudelft.trustchain.detoks.Token
import org.junit.Assert.assertEquals
import org.junit.Test

class TokenTest {
    @Test
    fun testToString() {
        val uniqueId = "12345"
        val token = Token(uniqueId, 0)

        assertEquals("$uniqueId,${0}", token.toString())
    }
}


