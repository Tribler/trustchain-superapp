package nl.tudelft.trustchain.detoks.token

import nl.tudelft.trustchain.detoks.helpers.DateFormatter
import nl.tudelft.trustchain.detoks.util.CommunityConstants
import java.util.*

class UpvoteTokenValidator{

    companion object {
        fun validateToken(upvoteToken: UpvoteToken) : Boolean {
            val tokenDate = DateFormatter.stringToDate(upvoteToken.date)
            val currentDate = Date()

            return upvoteToken.tokenID > -1 && upvoteToken.tokenID < CommunityConstants.DAILY_MINT_LIMIT
                && tokenDate.before(currentDate)
                // TODO Resolve this clauses
                // && upvoteToken.publicKeyMinter.exists()
                // && upvoteToken.video.exists()
        }
    }
}
