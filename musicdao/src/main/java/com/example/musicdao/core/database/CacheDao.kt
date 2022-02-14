package com.example.musicdao.core.database

import androidx.room.*
import com.example.musicdao.core.database.entities.AlbumEntity

@Dao
interface CacheDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(infos: AlbumEntity)

    @Query("DELETE FROM AlbumEntity WHERE id IS :id")
    suspend fun delete(id: String)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(entity: AlbumEntity)

    @Query("SELECT * FROM AlbumEntity")
    suspend fun getAll(): List<AlbumEntity>

    @Query("SELECT * FROM AlbumEntity WHERE id is :id")
    suspend fun get(id: String): AlbumEntity

    @Query("SELECT * FROM AlbumEntity WHERE artist LIKE :keyword OR title LIKE :keyword")
    suspend fun localSearch(keyword: String): List<AlbumEntity>
}
