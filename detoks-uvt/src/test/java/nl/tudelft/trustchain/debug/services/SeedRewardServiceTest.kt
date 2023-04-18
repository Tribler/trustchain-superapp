package nl.tudelft.trustchain.debug.services

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.tudelft.trustchain.detoks.services.SeedRewardService
import nl.tudelft.trustchain.detoks.token.UpvoteToken
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test suite for the [SeedRewardService] class
 */
@RunWith(AndroidJUnit4::class)
public class SeedRewardServiceTest {

    @Test
    public fun handleRewardTest() {
        val context =  ApplicationProvider.getApplicationContext<Context>()
        val seedRewardService = SeedRewardService(context)

        val upvoteTokenList = ArrayList<UpvoteToken>()
        Assert.assertFalse(seedRewardService.handleReward(upvoteTokenList))
    }
}
