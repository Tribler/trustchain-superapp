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
//@RunWith(AndroidJUnit4::class)
//class ExampleUnitTest {
//    @Test
//    fun addition_isCorrect() {
//        assertEquals(4, 2 + 2)
//    }
//}
//
//class TokenTest {
//    @Test
//    fun `test toString`() {
//        val uniqueId = "12345"
//        val publicKey = byteArrayOf(1)
//        val token = Token(uniqueId, publicKey)
//
//        assertEquals("$uniqueId,${publicKey.contentToString()}", token.toString())
//    }
//}

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("nl.tudelft.trustchain.debug.test", appContext.packageName)
    }
}



//@RunWith(AndroidJUnit4::class)
//class TokenStoreTest {
//
//    private lateinit var context: Context
//    private lateinit var store: TokenStore
//
//
//    @Before
//    fun setUp() {
//        context = InstrumentationRegistry.getInstrumentation().targetContext
//        store = TokenStore.getInstance(context)
//        store.removeAllTokens()
//    }
//
//    @Test
//    fun testAddAndGetAllTokens() {
//        val token1 = Token("123", byteArrayOf(1, 2, 3))
//        val token2 = Token("456", byteArrayOf(4, 5, 6))
//
//        store.addToken(token1.unique_id, token1.public_key.contentToString())
//        store.addToken(token2.unique_id, token2.public_key.contentToString())
//        val tokens = store.getAllTokens()
//
//        assertEquals(listOf(token1, token2), tokens)
//    }
//
//
//    @Test
//    fun testRemoveTokenByID() {
//        val token1 = Token("123", byteArrayOf(1, 2, 3))
//        val token2 = Token("456", byteArrayOf(4, 5, 6))
//        store.addToken(token1.unique_id, token1.public_key.contentToString())
//        store.addToken(token2.unique_id, token2.public_key.contentToString())
//
//        store.removeTokenByID(token1.unique_id)
//        val tokens = store.getAllTokens()
//
//        assertEquals(listOf(token2), tokens)
//    }
//
//    @Test
//    fun testRemoveAllTokens() {
//        val token1 = Token("123", byteArrayOf(1, 2, 3))
//        val token2 = Token("456", byteArrayOf(4, 5, 6))
//        store.addToken(token1.unique_id, token1.public_key.contentToString())
//        store.addToken(token2.unique_id, token2.public_key.contentToString())
//
//        store.removeAllTokens()
//        val tokens = store.getAllTokens()
//
//        assertTrue(tokens.isEmpty())
//    }
//
//    @Test
//    fun testGetBalance() {
//        val token1 = Token("123", byteArrayOf(1, 2, 3))
//        val token2 = Token("456", byteArrayOf(4, 5, 6))
//        store.addToken(token1.unique_id, token1.public_key.contentToString())
//        store.addToken(token2.unique_id, token2.public_key.contentToString())
//
//        val balance = store.getBalance()
//
//        assertEquals(2, balance)
//    }
//
//    @Test
//    fun testGetSingleToken() {
//        val token1 = Token("123", byteArrayOf(1, 2, 3))
//        val token2 = Token("456", byteArrayOf(4, 5, 6))
//
//        store.addToken(token1.unique_id, token1.public_key.contentToString())
//        store.addToken(token2.unique_id, token2.public_key.contentToString())
//
//        val singleToken = store.getSingleToken()
//
//        assertTrue(singleToken == token1 || singleToken == token2)
//    }
//}
