package nl.tudelft.trustchain.detoks_engine.db

import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import nl.tudelft.token_engine.sqldelight.Database

class TokenStore private constructor(context: Context) {
    private val driver = AndroidSqliteDriver(Database.Schema, context, "detokstokens.db")
    private val db = Database(driver)

    init {
        db.dbDetoksTokenStoreQueries.createTokenTable()
    }

    fun storeToken(token: String): Unit {
        db.dbDetoksTokenStoreQueries.storeToken(null, token)
    }

    fun getAllToken(): List<String> =
        db.dbDetoksTokenStoreQueries.getAllTokens{ _, token -> token }.executeAsList()

    fun removeToken(token: String) {
        db.dbDetoksTokenStoreQueries.removeToken(token)
    }

    companion object {
        private lateinit var tokenStore: TokenStore

        fun getInstance(context: Context): TokenStore {
            if (!::tokenStore.isInitialized) {
                tokenStore = TokenStore(context)
            }
            return tokenStore
        }
    }

}
