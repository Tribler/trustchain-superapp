package nl.tudelft.trustchain.debug.token

import nl.tudelft.trustchain.detoks.helpers.DateFormatter
import nl.tudelft.trustchain.detoks.token.UpvoteToken
import nl.tudelft.trustchain.detoks.token.UpvoteTokenValidator
import nl.tudelft.trustchain.detoks.util.CommunityConstants
import org.junit.Assert
import org.junit.Test
import java.time.LocalDate
import java.util.*

class UpvoteTokenValidatorTest {

    private val videoId = "videoID"
    private val publicKey = "pubKey"

    private val maxId = CommunityConstants.DAILY_MINT_LIMIT

    @Test
    fun tokenValidations() {
        val negativeId = UpvoteToken(-1, DateFormatter.todayAsString(), publicKey, videoId, publicKey)
        val idTooLarge = UpvoteToken(maxId, DateFormatter.todayAsString(), publicKey, videoId, publicKey)

        val tomorrow = LocalDate.now().plusDays(1).toString()
        val dateTooLate = UpvoteToken(maxId, tomorrow, publicKey, videoId, publicKey)

        val valid = UpvoteToken(maxId - 1, DateFormatter.todayAsString(), publicKey, videoId, publicKey)

        Assert.assertFalse("Id cannot be negative", UpvoteTokenValidator.validateToken(negativeId))
        Assert.assertFalse("Id cannot be larger than the limit", UpvoteTokenValidator.validateToken(idTooLarge))
        Assert.assertFalse("Token cannot be minted in the future", UpvoteTokenValidator.validateToken(dateTooLate))
        Assert.assertTrue("Token $valid should be valid", UpvoteTokenValidator.validateToken(valid))
    }
}
