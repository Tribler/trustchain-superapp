package nl.tudelft.trustchain.debug.token

import nl.tudelft.trustchain.detoks.db.SentTokenManager
import nl.tudelft.trustchain.detoks.exception.InvalidMintException
import nl.tudelft.trustchain.detoks.helpers.DateFormatter
import nl.tudelft.trustchain.detoks.token.UpvoteToken
import nl.tudelft.trustchain.detoks.token.UpvoteTokenFactory
import nl.tudelft.trustchain.detoks.util.CommunityConstants
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import java.time.LocalDate

/**
 * Test suite for the [UpvoteTokenFactory] class
 */
class UpvoteTokenFactoryTest {

    private lateinit var tokenFactory: UpvoteTokenFactory
    private lateinit var manager: SentTokenManager

    private val videoId = "videoID"
    private val publicKey = "myPublicKey"
    private val seederPubKey = "SeedPublicKey"

    @Before
    fun setup() {
        manager  = mock(SentTokenManager::class.java)
        tokenFactory = UpvoteTokenFactory(manager)

    }

    @Test
    fun mintFirstTokenTest() {
        `when`(manager.getLastToken()).doReturn(null)
        `when`(manager.addSentToken(any())).doReturn(true)
        val newToken = tokenFactory.tryMintToken(videoId, publicKey, seederPubKey)
        Assert.assertNotNull(newToken)
        Assert.assertEquals("The new token should have id 0", 0, newToken.tokenID)
        Assert.assertEquals("The new token should be minted today", DateFormatter.todayAsString(), newToken.date)
        Assert.assertEquals("The token should have the correct videoID", videoId, newToken.videoID)
        Assert.assertEquals("The token should have the correct public key", publicKey, newToken.publicKeyMinter)
        Assert.assertEquals("The token should have the correct seeder public key", seederPubKey, newToken.publicKeySeeder)
    }

    @Test
    fun continueSequenceTokenTest() {
        val oldTokenID = 5
        val oldToken = UpvoteToken(oldTokenID, DateFormatter.todayAsString(), videoId, publicKey, seederPubKey)
        val expectedTokenId = oldTokenID + 1
        `when`(manager.getLastToken()).doReturn(oldToken)
        `when`(manager.addSentToken(any())).doReturn(true)
        val newToken = tokenFactory.tryMintToken(videoId, publicKey, seederPubKey)

        Assert.assertNotNull(newToken)
        Assert.assertEquals("The new token should have id $expectedTokenId", expectedTokenId, newToken.tokenID)
        Assert.assertEquals("The new token should be minted today", DateFormatter.todayAsString(), newToken.date)
        Assert.assertEquals("The token should have the correct videoID", videoId, newToken.videoID)
        Assert.assertEquals("The token should have the correct public key", publicKey, newToken.publicKeyMinter)
        Assert.assertEquals("The token should have the correct seeder public key", seederPubKey, newToken.publicKeySeeder)
    }


    @Test
    fun startNewSequenceTokenTest() {

        val yesterday = LocalDate.now().plusDays(-1).toString()
        val oldToken = UpvoteToken(5, yesterday, videoId, publicKey, seederPubKey)
        `when`(manager.getLastToken()).doReturn(oldToken)
        `when`(manager.addSentToken(any())).doReturn(true)
        val newToken = tokenFactory.tryMintToken(videoId, publicKey, seederPubKey)
        Assert.assertNotNull(newToken)
        Assert.assertEquals("The new token should have id 0", 0, newToken.tokenID)
        Assert.assertEquals("The new token should be minted today", DateFormatter.todayAsString(), newToken.date)
        Assert.assertEquals("The token should have the correct videoID", videoId, newToken.videoID)
        Assert.assertEquals("The token should have the correct public key", publicKey, newToken.publicKeyMinter)
        Assert.assertEquals("The token should have the correct seeder public key", seederPubKey, newToken.publicKeySeeder)
    }

    @Test
    fun exceedingMintLimitTest() {
        val oldTokenID = CommunityConstants.DAILY_MINT_LIMIT
        val oldToken = UpvoteToken(oldTokenID, DateFormatter.todayAsString(), videoId, publicKey, seederPubKey)
        `when`(manager.getLastToken()).doReturn(oldToken)
        `when`(manager.addSentToken(any())).doReturn(true)
        try {
            tokenFactory.tryMintToken(videoId, publicKey, seederPubKey)
            // The execution should not reach this part
            Assert.fail()
        } catch (e: InvalidMintException) {
            Assert.assertEquals("Mint limit exceeded", e.message)
            verify(manager, times(0)).addSentToken(any())
        }
    }

    @Test
    fun invalidDatabaseSaveTest() {
        `when`(manager.getLastToken()).doReturn(null)
        `when`(manager.addSentToken(any())).doReturn(false)
        try {
            tokenFactory.tryMintToken(videoId, publicKey, seederPubKey)
            // The execution should not reach this part
            Assert.fail()
        } catch (e: InvalidMintException) {
            Assert.assertEquals("Could not add the minted token to the database", e.message)
            verify(manager, times(1)).addSentToken(any())
        }
    }

    @Test
    fun mintNoTokens() {
        `when`(manager.getLastToken()).doReturn(null)
        `when`(manager.addSentToken(any())).doReturn(true)

        val upvoteTokenList = tokenFactory.tryMintMultipleTokens(videoId, publicKey, seederPubKey, 0)

        Assert.assertTrue("The list should be empty", upvoteTokenList.isEmpty())
        verify(manager, times(0)).addSentToken(any())
        verify(manager, times(0)).getLastToken()
    }

    @Test
    fun mintMultipleTokens() {

        val amountOfTokens = 5
        val expectedList: ArrayList<UpvoteToken> = ArrayList()

        // Return null on the first call
        val lastTokenMock = `when`(manager.getLastToken())
            .doReturn(null)

        for (i in 0 until amountOfTokens) {
            val token = UpvoteToken(i, DateFormatter.todayAsString(), publicKey, videoId, seederPubKey)
            expectedList.add(token)
            // Return this token in subsequent calls
            lastTokenMock.doReturn(token)
        }



        `when`(manager.addSentToken(any())).doReturn(true)


        val resultList = tokenFactory.tryMintMultipleTokens(videoId, publicKey, seederPubKey, amountOfTokens)
        Assert.assertEquals("There should be $amountOfTokens tokens", amountOfTokens, resultList.size)

        for (i in 0 until amountOfTokens) {
            Assert.assertTrue("The tokens at index $i should be equal",
                expectedList[i] == resultList[i]
            )
        }
        verify(manager, times(amountOfTokens)).addSentToken(any())
        verify(manager, times(amountOfTokens)).getLastToken()
    }


}
