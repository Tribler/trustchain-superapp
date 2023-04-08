package nl.tudelft.trustchain.offlinemoney.db

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

    @Query("SELECT COUNT(*) FROM tokens_table WHERE token_value = :token_value")
    fun getCountTokensOfValue(token_value: Double) : Int

    // Insert transactions into the database
    @Insert
    suspend fun insertToken(tokens: Token)

    // Delete transactions
    @Query("DELETE FROM tokens_table WHERE token_id = :token_id")
    fun deleteToken(token_id: String)

}
