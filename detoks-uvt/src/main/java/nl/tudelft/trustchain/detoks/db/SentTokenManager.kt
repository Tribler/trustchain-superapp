package nl.tudelft.trustchain.detoks.db
import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import nl.tudelft.trustchain.detoks.Database
import nl.tudelft.trustchain.detoks.token.UpvoteToken

/**
 * TrustStore stores the trust scores of other wallets.
 * When we receive EuroTokens, the sender also sends its latest
 * 50 public keys of transactions he/she made. For every public key,
 * a trust score is maintained in order to build the web of trust.
 */
class SentTokenManager (context: Context) {
    private val driver = AndroidSqliteDriver(Database.Schema, context, "sent_upvote_tokens.db")
    private val database = Database(driver)

    /**
     * Maps the keys and accompanying trust scores out of the database into a kotlin [UpvoteToken] object.
     */
    private val sentTokenMapper = {
            token_id : Long,
            date : String,
            public_key : String,
            video_id : Long
        ->
        UpvoteToken(
            token_id.toInt(),
            date,
            public_key,
            video_id.toInt()
        )
    }

    /**
     * Retrieve all [UpvoteToken]s from the database.
     */
    fun getAllTokens() : List<UpvoteToken> {
        return database.dbUpvoteTokenQueries.getAll<UpvoteToken>(sentTokenMapper).executeAsList()
    }

    fun getLastToken() : UpvoteToken {
        return database.dbUpvoteTokenQueries.getLast<UpvoteToken>(sentTokenMapper).executeAsList().first()
    }

    /**
     * Initialize the database.
     */
    fun createContactStateTable() {
        database.dbUpvoteTokenQueries.createSentUpvoteTokensTable()
    }

    companion object {
        private lateinit var instance: SentTokenManager
        fun getInstance(context: Context): SentTokenManager {
            if (!::instance.isInitialized) {
                instance = SentTokenManager(context)
            }
            return instance
        }
    }
}
