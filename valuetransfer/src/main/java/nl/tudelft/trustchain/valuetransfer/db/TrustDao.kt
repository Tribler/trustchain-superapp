package nl.tudelft.trustchain.valuetransfer.db

import androidx.room.*
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.trustchain.valuetransfer.entity.TrustScore

@Dao
interface TrustDao {
    @Query("SELECT * FROM TrustScore")
    suspend fun getAll(): List<TrustScore>

    @Query("SELECT * FROM TrustScore ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomN(limit: Int): List<TrustScore>

    @Query("SELECT * FROM TrustScore WHERE publicKey == :key")
    suspend fun getByKey(key: PublicKey): TrustScore?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg trustScore: TrustScore)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<TrustScore>)

    @Delete
    suspend fun delete(trustScore: TrustScore)

    @Update
    suspend fun update(vararg score: TrustScore)
}
