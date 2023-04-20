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
            tokenIntId: Long,
        ->
        Token(
            id, tokenIntId.toInt()
        )
    }

    /**
     * Retrieve all [Token]s from the database.
     */
    fun getAllTokens() : List<Token> {
        return database.tokenStoreQueries.getAllTokens(tokenMapper).executeAsList()
    }

    /**
     * Removes a list of tokens from the database
     * @param tokenList: list of tokens to be removed
     */
    fun removeTokenList(tokenList : List<String>){
        return database.tokenStoreQueries.removeTokenList(tokenList)
    }

    /**
     * Adds a list of tokens to the database
     * @param tokens: list of tokens to be added
     */
    fun addTokenList(tokens : List<Token>, check: Boolean = false) {
        database.tokenStoreQueries.transaction {
            tokens.forEach { token ->
                if(check){
                    database.tokenStoreQueries.addTokenWithCheck(
                        id = token.unique_id,
                        tokenIntId = token.tokenIntId.toLong()
                    )
                }
                else{
                    database.tokenStoreQueries.addToken(
                        id = token.unique_id,
                        tokenIntId = token.tokenIntId.toLong())
                }
            }
        }

    }

    /**
     * Adds a transaction to the database
     */
    fun addToken(id : String, tokenIntId: Long, check: Boolean = false){
        if(check){
            database.tokenStoreQueries.addTokenWithCheck(id, tokenIntId)
        }
        else{
            database.tokenStoreQueries.addToken(id, tokenIntId)
        }
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
     * Retreives a single token from the database
     */
    fun getSingleToken(): Token {
        return database.tokenStoreQueries.getToken(tokenMapper).executeAsOne()
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



