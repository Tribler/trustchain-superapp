package nl.tudelft.trustchain.detoks.db
import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import nl.tudelft.detoks.sqldelight.Database
import nl.tudelft.trustchain.detoks.Token

class TokenStore(context: Context){
    private val driver = AndroidSqliteDriver(Database.Schema, context, "tokenstore.db")
    private val database = Database(driver)

    /**
     * Maps the keys and accompanying trust scores out of the database into a kotlin [Transaction] object.
     */
    private val tokenMapper = {
            id : String,
            publicKey: String
        ->
        Token(
            id, publicKey.toByteArray()
        )
    }

    /**
     * Retrieve all [Token]s from the database.
     */
    fun getAllTokens() : List<Token> {
        return database.tokenStoreQueries.getAllTokens(tokenMapper).executeAsList()
    }

    /**
     * Adds a transaction to the database
     */
    fun addToken(id : String, publicKey: String){
        database.tokenStoreQueries.addToken(id, publicKey)
    }

    /**
     * Deletes a transaction from the database based on an ID
     */
    fun removeTokenByID(id : String){
        database.tokenStoreQueries.removeTokenByID(id)
    }


    /**
     * Deletes all transactions from the database
     */
    fun removeAllTokens() {
        database.tokenStoreQueries.removeAllTokens()
    }

    /**
     * Removes a single token from the database
     */
    fun removeToken() {
        database.tokenStoreQueries.removeToken()
    }

    /**
     * Checks the balance of the user
     */
    fun getBalance() : Int {
        return database.tokenStoreQueries.getBalance().executeAsOne().toInt()
    }

    companion object {
        private lateinit var instance: TokenStore
        fun getInstance(context: Context): TokenStore {
            if (!::instance.isInitialized) {
                instance = TokenStore(context)
            }
            return instance
        }
    }
}



