package nl.tudelft.trustchain.detoks.token

import android.content.Context
import android.widget.Toast
import nl.tudelft.trustchain.detoks.db.SentTokenManager
import nl.tudelft.trustchain.detoks.helpers.DateFormatter
import nl.tudelft.trustchain.detoks.exception.InvalidMintException
import java.util.*

class UpvoteToken constructor(tokenID: Int, date: String, publicKeyMinter: String, videoID: Int) {

    val tokenID = tokenID
    val date = date
    val publicKeyMinter = publicKeyMinter
    val videoID = videoID


    companion object {
        public fun tryMintToken(context: Context, videoID: Int, publicKey: String): UpvoteToken {
            SentTokenManager(context).createSentUpvoteTokensTable()
            val lastUpvoteToken = SentTokenManager(context).getLastToken()
            // Check if we have sent a token already today
            val today = DateFormatter.startOfToday()
            val newToken: UpvoteToken

            // Check if a new sequence should be started
            if (lastUpvoteToken == null || DateFormatter.stringToDate(lastUpvoteToken.date).before(today)) {
                newToken = UpvoteToken(0, DateFormatter.todayAsString(), publicKey, videoID)
            } else if (lastUpvoteToken.tokenID > -1 && lastUpvoteToken.tokenID < 9) {
                val nextId = lastUpvoteToken.tokenID + 1
                newToken = UpvoteToken(nextId, DateFormatter.todayAsString(), publicKey, videoID)
            } else {
                throw InvalidMintException("Mint limit exceeded")
            }

            return newToken
        }
    }
}
