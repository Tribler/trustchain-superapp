package nl.tudelft.trustchain.offlinedigitaleuro.db

import androidx.room.*


@Entity(tableName = "tokens_table")
class Token(
    @PrimaryKey() var token_id: String,
    @ColumnInfo(name = "token_value") var token_value: Double,
    @ColumnInfo(name = "token_data") var token_data: ByteArray,
)


@Dao
interface TokensDao {
    // Retrieve all tokens in database
    @Query("SELECT * FROM tokens_table ORDER BY token_value")
    fun getAllTokens() : Array<Token>

    // Get all token_types from from database
    @Query("SELECT * FROM tokens_table WHERE token_value = :token_value")
    fun getAllTokensOfValue(token_value: Double) : Array<Token>

    // Get an amount of token_types from from database
    @Query("SELECT * FROM tokens_table WHERE token_value = :token_value LIMIT :count")
    fun getCountOfTokensOfValue(token_value: Double, count: Int) : Array<Token>

    // get how many tokens are of a certain value from database
    @Query("SELECT COUNT(*) FROM tokens_table WHERE token_value = :token_value")
    fun getCountTokensOfValue(token_value: Double) : Int

    @Query("SELECT * FROM tokens_table WHERE token_id = :token_id")
    fun getSpecificToken(token_id: String) : Array<Token>

    // Insert transactions into the database
    @Insert
    suspend fun insertToken(tokens: Token)

    // Delete transactions
    @Query("DELETE FROM tokens_table WHERE token_id = :token_id")
    fun deleteToken(token_id: String)

}
