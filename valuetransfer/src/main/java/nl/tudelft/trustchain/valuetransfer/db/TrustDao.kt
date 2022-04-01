package nl.tudelft.trustchain.valuetransfer.db

import androidx.room.*
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.trustchain.valuetransfer.entity.TrustScore

@Dao
interface TrustDao {
    @Query("SELECT * FROM TrustScore")
    suspend fun getAll(): List<TrustScore>

    @Query("SELECT * FROM TrustScore WHERE publicKey == :key")
    suspend fun getByKey(key: PublicKey): TrustScore?

    @Query("SELECT * FROM TrustScore WHERE publicKey == :key LIMIT :limit")
    suspend fun getByKeyLimit(key: PublicKey, limit: Int): List<TrustScore>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg trustScore: TrustScore)

    @Delete
    suspend fun delete(trustScore: TrustScore)

    @Update
    suspend fun update(vararg score: TrustScore)
}
