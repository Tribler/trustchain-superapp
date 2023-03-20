package nl.tudelft.trustchain.detoks.token

import android.content.Context
import android.widget.Toast
import nl.tudelft.trustchain.detoks.db.SentTokenManager
import nl.tudelft.trustchain.detoks.helpers.DateFormatter
import nl.tudelft.trustchain.detoks.exception.InvalidMintException
import java.util.*

class UpvoteToken constructor(
    val tokenID: Int,
    val date: String,
    val publicKeyMinter: String,
    val videoID: String
) {

    companion object {
        fun tryMintToken(context: Context, videoID: String, publicKey: String): UpvoteToken {
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
