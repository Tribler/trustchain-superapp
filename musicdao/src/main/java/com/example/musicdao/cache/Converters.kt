package com.example.musicdao.cache

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.example.musicdao.cache.entities.SongEntity
import com.google.common.reflect.TypeToken

@ProvidedTypeConverter
class Converters(
    private val jsonParser: JsonParser
) {
    @TypeConverter
    fun fromSongsJson(json: String): List<SongEntity> {
        return jsonParser.fromJson<ArrayList<SongEntity>>(
            json,
            object : TypeToken<ArrayList<SongEntity>>() {}.type
        ) ?: emptyList()
    }

    @TypeConverter
    fun toSongsJson(meanings: List<SongEntity>): String {
        return jsonParser.toJson(
            meanings,
            object : TypeToken<ArrayList<SongEntity>>() {}.type
        ) ?: "[]"
    }
}
