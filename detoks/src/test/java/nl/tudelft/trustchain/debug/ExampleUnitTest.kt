package nl.tudelft.trustchain.debug

import nl.tudelft.trustchain.detoks.Token
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
}

class TokenTest {

    @Test
    fun `test toString`() {
        val uniqueId = "12345"
        val publicKey = byteArrayOf(1)
        val token = Token(uniqueId, publicKey)

        assertEquals("$uniqueId,$publicKey", token.toString())
    }
}
