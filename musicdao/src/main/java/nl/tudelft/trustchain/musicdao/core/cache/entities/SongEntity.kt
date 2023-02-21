package nl.tudelft.trustchain.musicdao.core.cache.entities

import androidx.room.Entity
import nl.tudelft.trustchain.musicdao.core.repositories.model.Song
import java.io.File

@Entity
data class SongEntity(
    val title: String,
    val artist: String,
    val file: String
) {
    fun toSong(): Song {
        return Song(
            title = title,
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
