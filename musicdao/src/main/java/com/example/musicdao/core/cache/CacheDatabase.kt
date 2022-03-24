package com.example.musicdao.core.cache

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.musicdao.core.cache.entities.AlbumEntity
import com.example.musicdao.core.cache.parser.Converters

@Database(
    entities = [AlbumEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CacheDatabase : RoomDatabase() {
    abstract val dao: CacheDao
}
