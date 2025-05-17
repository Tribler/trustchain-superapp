package nl.tudelft.trustchain.musicdao.core.cache.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import nl.tudelft.trustchain.musicdao.core.cache.entities.AlbumEntity

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums")
    fun getAll(): List<AlbumEntity>

    @Query("SELECT * FROM albums")
    fun getAllLiveData(): LiveData<List<AlbumEntity>>

    @Query("SELECT * FROM albums")
    fun getAllFlow(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE publisher = :publicKey")
    suspend fun getFromArtist(publicKey: String): List<AlbumEntity>

    @Query("SELECT * FROM albums WHERE id = :id")
    suspend fun get(id: String): AlbumEntity?

    @Query("SELECT * FROM albums WHERE id = :id")
    fun getLiveData(id: String): LiveData<AlbumEntity>

    @Query("SELECT * FROM albums WHERE title LIKE '%' || :keyword || '%' OR artist LIKE '%' || :keyword || '%'")
    suspend fun localSearch(keyword: String): List<AlbumEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(album: AlbumEntity)

    @Delete
    suspend fun delete(album: AlbumEntity)
} 