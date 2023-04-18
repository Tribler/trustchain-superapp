package nl.tudelft.trustchain.detoks_engine.db

import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import nl.tudelft.token_engine.sqldelight.Database
class LastTokenStore private constructor(context: Context){
        private val driver = AndroidSqliteDriver(Database.Schema, context, "detoklasttoken.db")
        private val db = Database(driver)

        init {
            db.dbDetoksLastTokenQueries.createTokenTable()
        }

        fun saveLastToken(token: String): Unit {
            db.dbDetoksLastTokenQueries.setLastToken(token)
        }

        fun getLastToken(): String? {
            return db.dbDetoksLastTokenQueries.getLastToken().executeAsOneOrNull()
        }

        companion object {
            private lateinit var tokenStore: LastTokenStore

            fun getInstance(context: Context): LastTokenStore {
                if (!::tokenStore.isInitialized) {
                    tokenStore = LastTokenStore(context)
                }
                return tokenStore
            }
        }
}
