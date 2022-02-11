package com.example.musicdao.cache

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.musicdao.cache.entities.AlbumEntity

@Database(
    entities = [AlbumEntity::class],
    version = 3
)
@TypeConverters(Converters::class)
abstract class CacheDatabase : RoomDatabase() {
    abstract val dao: CacheDao
}
