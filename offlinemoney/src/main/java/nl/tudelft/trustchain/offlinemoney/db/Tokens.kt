package nl.tudelft.trustchain.offlinemoney.db

import androidx.room.*


@Entity(tableName = "tokens_table")
class Tokens(
    @PrimaryKey() var token_id: String,
    @ColumnInfo(name = "token_value") var token_value: Double,
    @ColumnInfo(name = "token_data") var token_data: ByteArray,
)


@Dao
interface TokensDao {
    // Get all token_types from from database
    @Query("SELECT * FROM tokens_table")
    fun getAllTokensOfType(token_type: String)

    // Insert transactions into the database
    @Insert
    suspend fun insertToken(tokens: Tokens)

    // Delete transactions
    @Delete
    suspend fun deleteToken(tokens: Tokens)

//    @Query("DELETE FROM tokens_table")
//    suspend fun deleteToken()

}
