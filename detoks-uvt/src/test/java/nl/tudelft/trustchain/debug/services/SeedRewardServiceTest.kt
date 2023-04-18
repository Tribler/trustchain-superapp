package nl.tudelft.trustchain.debug.services

import nl.tudelft.trustchain.detoks.db.OwnedTokenManager
import nl.tudelft.trustchain.detoks.helpers.DateFormatter
import nl.tudelft.trustchain.detoks.services.SeedRewardService
import nl.tudelft.trustchain.detoks.token.UpvoteToken
import nl.tudelft.trustchain.detoks.util.CommunityConstants
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.any

/**
 * Test suite for the [SeedRewardService] class
 */
class SeedRewardServiceTest {

    private lateinit var seedRewardService: SeedRewardService
    private lateinit var manager: OwnedTokenManager

    private val tokensExpected = CommunityConstants.SEED_REWARD_TOKENS

    @Before
    fun setup() {
        manager  = mock(OwnedTokenManager::class.java)
        `when`(manager.addReceivedToken(any())).doReturn(true)
        seedRewardService = SeedRewardService(manager)

    }

    @Test
    fun assertInitCalled() {
        verify(manager, times(1)).createOwnedUpvoteTokensTable()
    }

    @Test
    fun handleRewardEmptyListTest() {
        val upvoteTokenList = ArrayList<UpvoteToken>()
        Assert.assertFalse("Receiving no tokens should invalidate the seeding reward",seedRewardService.handleReward(upvoteTokenList))
    }

    @Test
    fun handleRewardOneTooShortTest() {

        val upvoteTokenList = ArrayList<UpvoteToken>()

        for (i in 0 until tokensExpected - 1) {
            val upvoteToken = UpvoteToken(i, DateFormatter.todayAsString(), "someKey", " ", "someOtherKey")
            upvoteTokenList.add(upvoteToken)
        }

        Assert.assertFalse("Receiving too little tokens should invalidate the seeding reward",seedRewardService.handleReward(upvoteTokenList))
        verify(manager, times(upvoteTokenList.size)).addReceivedToken(any())
    }

    @Test
    fun handleRewardJustEnoughTest() {

        val upvoteTokenList = ArrayList<UpvoteToken>()

        for (i in 0 until tokensExpected) {
            val upvoteToken = UpvoteToken(i, DateFormatter.todayAsString(), "someKey", " ", "someOtherKey")
            upvoteTokenList.add(upvoteToken)
        }

        Assert.assertTrue("Receiving enough tokens should validate the seed reward",seedRewardService.handleReward(upvoteTokenList))
        verify(manager, times(upvoteTokenList.size)).addReceivedToken(any())
    }
}
