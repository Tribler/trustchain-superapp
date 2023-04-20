package nl.tudelft.trustchain.detoks.db
import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import nl.tudelft.trustchain.detoks.Database
import nl.tudelft.trustchain.detoks.token.UpvoteToken

class OwnedTokenManager (context: Context) {
    private val driver = AndroidSqliteDriver(Database.Schema, context, "owned_upvote_tokens.db")
    private val database = Database(driver)

    /**
     * Maps the keys and accompanying [UpvoteToken]s out of the database into a kotlin [UpvoteToken] object.
     */
    private val ownedTokenMapper = {
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
        return database.dbUpvoteTokenQueries.getAllSentTokens<UpvoteToken>(ownedTokenMapper).executeAsList()
    }

    fun getLastToken() : UpvoteToken? {
        return database.dbUpvoteTokenQueries.getLastSentToken<UpvoteToken>(ownedTokenMapper).executeAsList().firstOrNull()
    }

    fun addReceivedToken(upvoteToken: UpvoteToken): Boolean {
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
    fun createOwnedUpvoteTokensTable() {
        database.dbUpvoteTokenQueries.createSentUpvoteTokensTable()
    }

    /**
     * Gets the latest upvoted videos
     */
    fun getLatestThreeUpvotedVideos() : List<String> {
        return database.dbUpvoteTokenQueries.getLatestThreeUpvotedVideos().executeAsList()
    }

    companion object {
        private lateinit var instance: OwnedTokenManager
        fun getInstance(context: Context): OwnedTokenManager {
            if (!::instance.isInitialized) {
                instance = OwnedTokenManager(context)
            }
            return instance
        }
    }
}
