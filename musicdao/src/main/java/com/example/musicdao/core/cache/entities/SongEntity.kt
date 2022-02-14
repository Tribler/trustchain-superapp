package com.example.musicdao.core.cache.entities

import androidx.room.Entity
import com.example.musicdao.core.model.Song
import java.io.File

@Entity
data class SongEntity(
    val title: String,
    val name: String,
    val artist: String,
    val file: String
) {
    fun toSong(): Song {
        return Song(
            title = title,
            name = name,
            artist = artist,
            file = file.let {
                File(it).let {
                    if (it.exists()) {
                        it
                    } else {
                        null
                    }
                }
            }

        )
    }
}
