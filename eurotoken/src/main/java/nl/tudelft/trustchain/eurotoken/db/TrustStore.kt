package nl.tudelft.trustchain.eurotoken.db
import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import nl.tudelft.eurotoken.sqldelight.Database
import nl.tudelft.trustchain.eurotoken.entity.TrustScore

/**
 * TrustStore stores the trust scores of other wallets.
 * When we receive EuroTokens, the sender also sends its latest
 * 50 public keys of transactions he/she made. For every public key,
 * a trust score is maintained in order to build the web of trust.
 */
class TrustStore (context: Context) {
    private val driver = AndroidSqliteDriver(Database.Schema, context, "eurotoken.db")
    private val database = Database(driver)

    private val messageMapper = {
            public_key : ByteArray,
            score : Long
        ->
        TrustScore(
            public_key,
            score.toInt()
        )
    }

    fun getAllScores() : List<TrustScore> {
        return database.dbTrustScoreQueries.getAll(messageMapper).executeAsList()
    }

    fun getScore(publicKey: ByteArray) : Long? {
        return database.dbTrustScoreQueries.getScore(publicKey).executeAsOneOrNull()
    }

    fun incrementTrust(publicKey: ByteArray) {
        val score : Long? = getScore(publicKey)

        if (score != null) {
            // Limit score to 100
            if(score.toInt() >= 100) {
                return
            }
            database.dbTrustScoreQueries.incrementScore(publicKey)
        } else {
            database.dbTrustScoreQueries.addScore(publicKey, 0)
        }
    }

    fun createContactStateTable() {
        database.dbTrustScoreQueries.createContactStateTable()
    }

    companion object {
        private lateinit var instance: TrustStore
        fun getInstance(context: Context): TrustStore {
            if (!::instance.isInitialized) {
                instance = TrustStore(context)
            }
            return instance
        }
    }
}
