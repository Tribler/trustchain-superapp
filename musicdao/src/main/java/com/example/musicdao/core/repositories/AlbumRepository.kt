package com.example.musicdao.core.repositories

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.musicdao.core.database.CacheDatabase
import com.example.musicdao.core.database.entities.AlbumEntity
import com.example.musicdao.core.ipv8.blocks.release_publish.ReleasePublishBlockRepository
import com.example.musicdao.core.model.Album
import javax.inject.Inject

/**
 * CRUD for any operations on Albums
 */
@RequiresApi(Build.VERSION_CODES.O)

class AlbumRepository @Inject constructor(
    private val database: CacheDatabase,
    private val releasePublishBlockRepository: ReleasePublishBlockRepository
) {

    suspend fun get(id: String): Album {
        return database.dao.get(id).toAlbum()
    }

    suspend fun getAlbums(): List<Album> {
        return database.dao.getAll().map { it.toAlbum() }
    }

    suspend fun searchAlbums(keyword: String): List<Album> {
        return database.dao.localSearch(keyword).map { it.toAlbum() }
    }

    fun create(
        releaseId: String,
        magnet: String,
        title: String,
        artist: String,
        releaseDate: String,
    ): Boolean {
        return releasePublishBlockRepository.create(
            releaseId = releaseId,
            magnet = magnet,
            title = title,
            artist = artist,
            releaseDate = releaseDate,
        ) != null
    }

    suspend fun refreshCache() {
        val releaseBlocks = releasePublishBlockRepository.getValidBlocks()
        releaseBlocks.forEach {
            database.dao.insert(
                AlbumEntity(
                    id = it.releaseId,
                    magnet = it.magnet,
                    title = it.title,
                    artist = it.artist,
                    publisher = it.publisher,
                    releaseDate = it.releaseDate,
                    songs = listOf(),
                    cover = null,
                    root = null
                )
            )
        }
    }

}
