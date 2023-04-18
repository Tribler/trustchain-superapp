package nl.tudelft.trustchain.debug.services

import nl.tudelft.trustchain.detoks.db.OwnedTokenManager
import nl.tudelft.trustchain.detoks.db.SentTokenManager
import nl.tudelft.trustchain.detoks.helpers.DateFormatter
import nl.tudelft.trustchain.detoks.services.UpvoteService
import nl.tudelft.trustchain.detoks.token.UpvoteToken
import nl.tudelft.trustchain.detoks.util.CommunityConstants
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn



/**
 * Test suite for the [UpvoteService] class
 */
class UpvoteServiceTest {

    private lateinit var upvoteService: UpvoteService
    private lateinit var ownedManager: OwnedTokenManager
    private lateinit var sentManager: SentTokenManager

    private lateinit var sentTokens : ArrayList<UpvoteToken>
    private lateinit var receivedTokens : ArrayList<UpvoteToken>

    private val upvotedList = mutableListOf("video1", "video2", "video3")
    private val sentList = mutableListOf("video1", "video2", "video3", "video4", "video5")
    private val tokensExpected = CommunityConstants.SEED_REWARD_TOKENS

    @Before
    fun setup() {
        ownedManager = mock(OwnedTokenManager::class.java)
        sentManager = mock(SentTokenManager::class.java)
        upvoteService = UpvoteService(ownedManager, sentManager)

        sentTokens = ArrayList()
        receivedTokens = ArrayList()

        // Set up received token manager mock
        `when`(ownedManager.addReceivedToken(any())).doReturn(true)

        `when`(ownedManager.getLatestThreeUpvotedVideos())
            .doReturn(upvotedList)

        `when`(sentManager.addSentToken(any())).doReturn(true)
        `when`(sentManager.getFiveLatestUpvotedVideos())
            .doReturn(sentList)

    }

    @Test
    fun assertInitCalled() {
        verify(ownedManager, times(1)).createOwnedUpvoteTokensTable()
        verify(sentManager, times(1)).createSentUpvoteTokensTable()
    }

    @Test
    fun getFiveLatestUpvotedVideosTest() {
        val result = upvoteService.getFiveLatestUpvotedVideos()
        Assert.assertEquals("The upvoted videos should be returned", sentList, result)
    }

    @Test
    fun getLatestThreeUpvotedVideosTest() {
        val result = upvoteService.getLatestThreeUpvotedVideos()
        Assert.assertEquals("The videos of received tokens should be returned", upvotedList, result)
    }

    @Test
    fun getValidUpvoteTokens() {
        val upvoteTokenList: ArrayList<UpvoteToken> = ArrayList()

        for (i in 0 until  5) {
            var upvoteToken : UpvoteToken
            if (i != 3) // Token at index 3 will be invalid
                upvoteToken = UpvoteToken(i, DateFormatter.todayAsString(), "someKey", "video", "someOtherKey")
            else
                upvoteToken = UpvoteToken(i, "This is no date!", "someKey", "video", "someOtherKey")

            upvoteTokenList.add(upvoteToken)
        }

        val listOfValidTokens = upvoteService.getValidUpvoteTokens(upvoteTokenList)

        Assert.assertEquals("There should be 4 valid tokens", 4, listOfValidTokens.size)
        Assert.assertTrue("Token with ID 0 should be in the list", listOfValidTokens.contains(upvoteTokenList[0]))
        Assert.assertTrue("Token with ID 1 should be in the list", listOfValidTokens.contains(upvoteTokenList[1]))
        Assert.assertTrue("Token with ID 2 should be in the list", listOfValidTokens.contains(upvoteTokenList[2]))
        Assert.assertFalse("Token with ID 3 should not be in the list", listOfValidTokens.contains(upvoteTokenList[3]))
        Assert.assertTrue("Token with ID 4 should be in the list", listOfValidTokens.contains(upvoteTokenList[4]))
    }

    @Test
    fun getRewardTokensEmptyListTest() {
        val upvoteTokenList = ArrayList<UpvoteToken>()
        val rewardTokens = upvoteService.getRewardTokens(upvoteTokenList)
        Assert.assertEquals("There should be no reward tokens", 0, rewardTokens.size)
        Assert.assertEquals("There should be no UpvoteTokens", 0, upvoteTokenList.size)
    }


    @Test
    fun getRewardTokensLessThanCommunityTest() {
        val upvoteTokenList = ArrayList<UpvoteToken>()

        for (i in 0 until tokensExpected - 1) {
            val upvoteToken = UpvoteToken(i, DateFormatter.todayAsString(), "someKey", " ", "someOtherKey")
            upvoteTokenList.add(upvoteToken)
        }

        val rewardTokens = upvoteService.getRewardTokens(upvoteTokenList)
        Assert.assertEquals("All tokens should be reward tokens", tokensExpected - 1, rewardTokens.size)
        Assert.assertTrue("The upvoteTokenList should be empty", upvoteTokenList.isEmpty())
    }

    @Test
    fun getRewardTokensMoreThanCommunityTest() {
        val upvoteTokenList = ArrayList<UpvoteToken>()
        val additionalTokens = 5
        for (i in 0 until tokensExpected + additionalTokens) {
            val upvoteToken = UpvoteToken(i, DateFormatter.todayAsString(), "someKey", " ", "someOtherKey")
            upvoteTokenList.add(upvoteToken)
        }

        val rewardTokens = upvoteService.getRewardTokens(upvoteTokenList)
        Assert.assertEquals("There should be enough reward tokens", tokensExpected, rewardTokens.size)
        Assert.assertEquals("The upvoteToken should have the remainder", additionalTokens, upvoteTokenList.size)

        for (upvoteToken in upvoteTokenList) {
            Assert.assertFalse("There should be no overlap between the lists", rewardTokens.contains(upvoteToken))
        }
    }

    @Test
    fun persistTokenTest() {
        val upvoteTokenList = ArrayList<UpvoteToken>()
        for (i in 0 until 5) {
            val upvoteToken = UpvoteToken(i, DateFormatter.todayAsString(), "someKey", " ", "someOtherKey")
            upvoteTokenList.add(upvoteToken)
        }

        upvoteService.persistTokens(upvoteTokenList)

        verify(ownedManager, times(upvoteTokenList.size)).addReceivedToken(any())
    }



}
