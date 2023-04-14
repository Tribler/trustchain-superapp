package nl.tudelft.trustchain.debug

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import nl.tudelft.trustchain.detoks.Token
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith



/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class TokenTest {
    @Test
    fun `test toString`() {
        val uniqueId = "12345"
        val publicKey = byteArrayOf(1)
        val token = Token(uniqueId, publicKey)

        assertEquals("$uniqueId,${publicKey.contentToString()}", token.toString())
    }
}


