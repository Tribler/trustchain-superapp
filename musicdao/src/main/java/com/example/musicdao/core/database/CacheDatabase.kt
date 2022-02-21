package com.example.musicdao.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.musicdao.core.database.entities.AlbumEntity
import com.example.musicdao.core.database.parser.Converters

@Database(
    entities = [AlbumEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CacheDatabase : RoomDatabase() {
    abstract val dao: CacheDao
}
