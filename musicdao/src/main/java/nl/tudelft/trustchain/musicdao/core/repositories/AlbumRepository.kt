package nl.tudelft.trustchain.musicdao.core.repositories

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import nl.tudelft.trustchain.musicdao.core.cache.CacheDatabase
import nl.tudelft.trustchain.musicdao.core.cache.entities.AlbumEntity
import nl.tudelft.trustchain.musicdao.core.ipv8.blocks.releasePublish.ReleasePublishBlock
import nl.tudelft.trustchain.musicdao.core.ipv8.blocks.releasePublish.ReleasePublishBlockRepository
import nl.tudelft.trustchain.musicdao.core.repositories.model.Album
import nl.tudelft.trustchain.musicdao.core.torrent.TorrentEngine
import javax.inject.Inject

/**
 * This class will be the class the application interacts with and will return
 * the data that the UI/CMD interface can work with.
 */
@RequiresApi(Build.VERSION_CODES.O)
class AlbumRepository @Inject constructor(
    private val database: CacheDatabase,
    private val releasePublishBlockRepository: ReleasePublishBlockRepository
) {

    suspend fun getAlbums(): List<Album> {
        return database.dao.getAll().map { it.toAlbum() }
    }

    fun getAlbumsFlow(): LiveData<List<Album>> {
        return database.dao.getAllLiveData().map { it.map { it.toAlbum() } }
    }

    suspend fun getAlbumsFromArtist(publicKey: String): List<Album> {
        return database.dao.getFromArtist(publicKey = publicKey).map { it.toAlbum() }
    }

    suspend fun searchAlbums(keyword: String): List<Album> {
        return database.dao.localSearch(keyword).map { it.toAlbum() }
    }

    suspend fun createAlbum(
        releaseId: String,
        magnet: String,
        title: String,
        artist: String,
        releaseDate: String
    ): Boolean {
        // Create and publish the Trustchain block.
        val block = releasePublishBlockRepository.create(
            releaseId = releaseId,
            magnet = magnet,
            title = title,
            artist = artist,
            releaseDate = releaseDate
        )

        // If successful, we optimistically add it to our local cache.
        block?.let {
            val transaction: ReleasePublishBlock = releasePublishBlockRepository.toBlock(block)
            database.dao.insert(
                AlbumEntity(
                    id = transaction.releaseId,
                    magnet = transaction.magnet,
                    title = transaction.title,
                    artist = transaction.artist,
                    publisher = transaction.publisher,
                    releaseDate = transaction.releaseDate,
                    songs = listOf(),
                    cover = null,
                    root = null,
                    isDownloaded = false,
                    infoHash = TorrentEngine.magnetToInfoHash(transaction.magnet),
                    torrentPath = null
                )
            )
        }

        return block != null
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
                    root = null,
                    isDownloaded = false,
                    infoHash = TorrentEngine.magnetToInfoHash(it.magnet),
                    torrentPath = null
                )
            )
        }
    }
}
