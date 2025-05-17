package nl.tudelft.trustchain.musicdao.core.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.DelicateCoroutinesApi
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
class AlbumRepository
    @Inject
    constructor(
        private val database: CacheDatabase,
        private val releasePublishBlockRepository: ReleasePublishBlockRepository,
        private val releaseRepository: ReleaseRepository
    ) {
        suspend fun getReleaseById(releaseId: String): Album? {
            return database.dao.get(releaseId)?.toAlbum()
        }

        suspend fun getAlbums(userPublicKey: String): List<Album> {
            return database.dao.getAll().map { entity ->
                val magnetLink = releaseRepository.getFullRelease(entity.id, userPublicKey)
                entity.toAlbum().copy(
                    magnet = magnetLink ?: "access_restricted"
                )
            }
        }

        fun getAlbumsFlow(userPublicKey: String): LiveData<List<Album>> {
            return database.dao.getAllLiveData().map { entities ->
                entities.map { entity ->
                    val magnetLink = releaseRepository.getFullRelease(entity.id, userPublicKey)
                    entity.toAlbum().copy(
                        magnet = magnetLink ?: "access_restricted"
                    )
                }
            }
        }

        suspend fun getAlbumsFromArtist(publicKey: String): List<Album> {
            return database.dao.getFromArtist(publicKey = publicKey).map { it.toAlbum() }
        }

        suspend fun searchAlbums(keyword: String): List<Album> {
            return database.dao.localSearch(keyword).map { it.toAlbum() }
        }

        @OptIn(DelicateCoroutinesApi::class)
        suspend fun createAlbum(
            releaseId: String,
            magnet: String,
            title: String,
            artist: String,
            releaseDate: String
        ): Boolean {
            // Create and publish the Trustchain block with preview
            val block = releasePublishBlockRepository.create(
                releaseId = releaseId,
                magnet = "",
                title = title,
                artist = artist,
                releaseDate = releaseDate
            )

            // If successful, add to local cache
            block?.let {
                val transaction: ReleasePublishBlock = releasePublishBlockRepository.toBlock(block)
                database.dao.insert(
                    AlbumEntity(
                        id = transaction.releaseId,
                        magnet = magnet,  // Store the magnet link locally for the artist
                        title = transaction.title,
                        artist = transaction.artist,
                        publisher = transaction.publisher,
                        releaseDate = transaction.releaseDate,
                        songs = listOf(),
                        cover = null,
                        root = null,
                        isDownloaded = false,
                        infoHash = TorrentEngine.magnetToInfoHash(magnet),
                        torrentPath = null
                    )
                )
            }

            return block != null
        }

        @OptIn(DelicateCoroutinesApi::class)
        suspend fun refreshCache() {
            val releaseBlocks = releasePublishBlockRepository.getValidBlocks()

            releaseBlocks.forEach {
                database.dao.insert(
                    AlbumEntity(
                        id = it.releaseId,
                        magnet = "access_restricted",  // Full release not available in preview
                        title = it.title,
                        artist = it.artist,
                        publisher = it.publisher,
                        releaseDate = it.releaseDate,
                        songs = listOf(),
                        cover = null,
                        root = null,
                        isDownloaded = false,
                        infoHash = null,  // Will be set when full release is accessed
                        torrentPath = null
                    )
                )
            }
        }
    }
