package nl.tudelft.trustchain.musicdao.core.cache.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import nl.tudelft.trustchain.musicdao.core.repositories.model.Album
import java.io.File
import java.time.Instant

@Entity
data class AlbumEntity(
    @PrimaryKey val id: String,
    val magnet: String,  // For artist: full magnet link, For others: "access_restricted"
    val title: String,
    val artist: String,
    val publisher: String,
    val releaseDate: String,
    val songs: List<SongEntity>,
    val cover: String?,
    val root: String?,
    val isDownloaded: Boolean,
    val infoHash: String?,
    val torrentPath: String?
) {
    fun toAlbum(): Album {
        return Album(
            id = id,
            magnet = magnet,
            title = title,
            artist = artist,
            publisher = publisher,
            releaseDate = Instant.parse(releaseDate),
            songs = songs.map { it.toSong() },
            cover =
                cover?.let { path ->
                    File(path).let {
                        if (it.exists()) {
                            it
                        } else {
                            null
                        }
                    }
                },
            root =
                root?.let { path ->
                    File(path).let {
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
