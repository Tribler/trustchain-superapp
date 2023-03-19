package nl.tudelft.trustchain.detoks.token

import nl.tudelft.trustchain.detoks.helpers.DateFormatter
import java.util.*

class UpvoteTokenValidator{

    companion object {
        fun ValidateToken(upvoteToken: UpvoteToken) : Boolean {
            val tokenDate = DateFormatter.stringToDate(upvoteToken.date)
            val currentDate = Date()

            return upvoteToken.tokenID > -1 && upvoteToken.tokenID < 10
                && tokenDate.before(currentDate)
                // TODO Resolve this clauses
                // && upvoteToken.publicKeyMinter.exists()
                // && upvoteToken.video.exists()
        }
    }
}
