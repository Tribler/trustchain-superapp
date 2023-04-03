package nl.tudelft.trustchain.detoks.db
import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import nl.tudelft.trustchain.detoks.Database
import nl.tudelft.trustchain.detoks.token.UpvoteToken

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
            video_id : String,
            public_key_seeder: String
        ->
        UpvoteToken(
            token_id.toInt(),
            date,
            public_key,
            video_id,
            public_key_seeder
        )
    }

    /**
     * Retrieve all [UpvoteToken]s from the database.
     */
    fun getAllTokens() : List<UpvoteToken> {
        return database.dbUpvoteTokenQueries.getAllSentTokens<UpvoteToken>(sentTokenMapper).executeAsList()
    }

    fun getLastToken() : UpvoteToken? {
        return database.dbUpvoteTokenQueries.getLastSentToken<UpvoteToken>(sentTokenMapper).executeAsList().firstOrNull()
    }

    fun addSentToken(upvoteToken: UpvoteToken): Boolean {
        database.dbUpvoteTokenQueries.addSentToken(
            upvoteToken.tokenID.toLong(),
            upvoteToken.date,
            upvoteToken.publicKeyMinter,
            upvoteToken.videoID,
            upvoteToken.publicKeySeeder
        )
        return true
    }

    /**
     * Initialize the database.
     */
    fun createSentUpvoteTokensTable() {
        database.dbUpvoteTokenQueries.createSentUpvoteTokensTable()
    }

    /**
     * Gets the five latest upvoted videos
     */
    fun getFiveLatestUpvotedVideos() : List<String> {
        return database.dbUpvoteTokenQueries.getFiveLatestUpvotedVideos().executeAsList()
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
