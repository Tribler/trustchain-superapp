package nl.tudelft.trustchain.eurotoken.db
import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import nl.tudelft.eurotoken.sqldelight.Database
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.trustchain.eurotoken.entity.TrustScore
import java.sql.Blob
import java.util.*

class TrustStore (context: Context) {
    private val driver = AndroidSqliteDriver(Database.Schema, context, "eurotoken.db")
    private val database = Database(driver)
    val contactsStore = ContactStore.getInstance(context)

    private val messageMapper = {
            public_key : ByteArray,
            score : Long
        ->
        TrustScore(
            defaultCryptoProvider.keyFromPublicBin(public_key),
            score.toInt()
        )
    }

    fun getAllScores() : List<TrustScore> {
        return database.dbTrustScoreQueries.getAll(messageMapper).executeAsList()
    }

    fun getScore(publicKey: String) : Long? {
        return database.dbTrustScoreQueries.getScore(publicKey.toByteArray()).executeAsOneOrNull()
    }

    fun incrementTrust(publicKey: String) {
        val score : Long? = getScore(publicKey)
        return if (score != null) {
            database.dbTrustScoreQueries.incrementScore(publicKey.toByteArray())
        } else {
            database.dbTrustScoreQueries.addScore(publicKey.toByteArray(), 0)
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
