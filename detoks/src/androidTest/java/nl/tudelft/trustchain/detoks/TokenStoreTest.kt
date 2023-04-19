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
        val token1 = Token("123", 0)
        val token2 = Token("456", 1)

        store.addToken(token1.unique_id, token1.tokenIntId.toLong(), true)
        store.addToken(token2.unique_id, token2.tokenIntId.toLong(), true)
        val tokens = store.getAllTokens()

        assertEquals(listOf(token1, token2), tokens)
    }


    @Test
    fun testRemoveTokenByID() {
        val token1 = Token("123", 0)
        val token2 = Token("456", 1)

        store.addToken(token1.unique_id, token1.tokenIntId.toLong(), true)
        store.addToken(token2.unique_id, token2.tokenIntId.toLong(), true)

        store.removeTokenByID(token1.unique_id)
        val tokens = store.getAllTokens()

        assertEquals(listOf(token2), tokens)
    }

    @Test
    fun testRemoveAllTokens() {

        val token1 = Token("123", 0)
        val token2 = Token("456", 1)

        store.addToken(token1.unique_id, token1.tokenIntId.toLong(), true)
        store.addToken(token2.unique_id, token2.tokenIntId.toLong(), true)

        store.removeAllTokens()
        val tokens = store.getAllTokens()

        assertTrue(tokens.isEmpty())
    }

    @Test
    fun testGetBalance() {

        val token1 = Token("123", 0)
        val token2 = Token("456", 1)

        store.addToken(token1.unique_id, token1.tokenIntId.toLong(), true)
        store.addToken(token2.unique_id, token2.tokenIntId.toLong(), true)

        val balance = store.getBalance()

        assertEquals(2, balance)
    }

    @Test
    fun testGetSingleToken() {

        val token1 = Token("123", 0)
        val token2 = Token("456", 1)

        store.addToken(token1.unique_id, token1.tokenIntId.toLong(), true)
        store.addToken(token2.unique_id, token2.tokenIntId.toLong(), true)

        val singleToken = store.getSingleToken()

        assertTrue(singleToken == token1 || singleToken == token2)
    }
}

