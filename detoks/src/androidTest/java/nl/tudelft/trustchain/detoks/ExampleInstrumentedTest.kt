package nl.tudelft.trustchain.detoks

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import nl.tudelft.trustchain.detoks.db.TokenStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
//@RunWith(AndroidJUnit4::class)
//class ExampleInstrumentedTest {
//    @Test
//    fun useAppContext() {
//        // Context of the app under test.
//        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
//        assertEquals("nl.tudelft.trustchain.detoks.test", appContext.packageName)
//    }
//}

@RunWith(AndroidJUnit4::class)
class TokenStoreTest {

    private lateinit var context: Context
    private lateinit var store: TokenStore


    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        store = TokenStore.getInstance(context)
        store.removeAllTokens()
    }

    @Test
    fun testAddAndGetAllTokens() {
        val token1 = Token("123", byteArrayOf(1, 2, 3), 10)
        val token2 = Token("456", byteArrayOf(4, 5, 6), 11)

        store.addToken(token1.unique_id, token1.public_key.contentToString(), token1.tokenIntId.toLong())
        store.addToken(token2.unique_id, token2.public_key.contentToString(), token2.tokenIntId.toLong())
        val tokens = store.getAllTokens()

        assertEquals(listOf(token1, token2).map{it.unique_id}, tokens.map{it.unique_id})
    }


    @Test
    fun testRemoveTokenByID() {
        val token1 = Token("123", byteArrayOf(1, 2, 3), 10)
        val token2 = Token("456", byteArrayOf(4, 5, 6), 11)

        store.addToken(token1.unique_id, token1.public_key.contentToString(), token1.tokenIntId.toLong())
        store.addToken(token2.unique_id, token2.public_key.contentToString(), token2.tokenIntId.toLong())

        store.removeTokenByID(token1.unique_id)
        val tokens = store.getAllTokens()

        assertEquals(listOf(token2).map{it.unique_id}, tokens.map{it.unique_id})
    }

    @Test
    fun testRemoveAllTokens() {
        val token1 = Token("123", byteArrayOf(1, 2, 3), 10)
        val token2 = Token("456", byteArrayOf(4, 5, 6), 11)

        store.addToken(token1.unique_id, token1.public_key.contentToString(), token1.tokenIntId.toLong())
        store.addToken(token2.unique_id, token2.public_key.contentToString(), token2.tokenIntId.toLong())

        store.removeAllTokens()
        val tokens = store.getAllTokens()

        assertTrue(tokens.isEmpty())
    }

    @Test
    fun testGetBalance() {
        val token1 = Token("123", byteArrayOf(1, 2, 3), 10)
        val token2 = Token("456", byteArrayOf(4, 5, 6), 11)

        store.addToken(token1.unique_id, token1.public_key.contentToString(), token1.tokenIntId.toLong())
        store.addToken(token2.unique_id, token2.public_key.contentToString(), token2.tokenIntId.toLong())

        val balance = store.getBalance()

        assertEquals(2, balance)
    }

    @Test
    fun testGetSingleToken() {
        val token1 = Token("123", byteArrayOf(1, 2, 3), 10)
        val token2 = Token("456", byteArrayOf(4, 5, 6), 11)

        store.addToken(token1.unique_id, token1.public_key.contentToString(), token1.tokenIntId.toLong())
        store.addToken(token2.unique_id, token2.public_key.contentToString(), token2.tokenIntId.toLong())

        val singleToken = store.getSingleToken()

        assertTrue(singleToken.unique_id == token1.unique_id || singleToken.unique_id == token2.unique_id)
    }
}

