package nl.tudelft.trustchain.offlinedigitaleuro.db

import androidx.room.*

@Entity(tableName = "weboftrust_table")
class WebOfTrust(
    @PrimaryKey() var public_key: String,
    @ColumnInfo(name = "trust_score") var trust_score: Int,
)



@Dao
interface WebOfTrustDAO {

    @Query("SELECT trust_score FROM weboftrust_table WHERE public_key = :public_key")
    fun getUserTrustScore(public_key: String): Int?

    @Query("SELECT * FROM weboftrust_table")
    fun getAllTrustScores(): Array<WebOfTrust>

    @Insert
    suspend fun insertUserTrustScore(user: WebOfTrust)

    @Query("UPDATE weboftrust_table SET trust_score = trust_score + :update_score_value WHERE public_key = :public_key")
    fun updateUserScore(public_key: String, update_score_value: Int)

}
