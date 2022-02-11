package com.example.musicdao.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.musicdao.cache.entities.AlbumEntity

@Dao
interface CacheDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(infos: AlbumEntity)

    @Query("DELETE FROM AlbumEntity WHERE id IS :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM AlbumEntity")
    suspend fun getAll(): List<AlbumEntity>

    @Query("SELECT * FROM AlbumEntity WHERE id is :id")
    suspend fun get(id: String): AlbumEntity
}
