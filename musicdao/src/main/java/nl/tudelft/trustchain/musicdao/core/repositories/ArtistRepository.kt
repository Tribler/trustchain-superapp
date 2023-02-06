package nl.tudelft.trustchain.musicdao.core.repositories

import android.os.Build
import androidx.annotation.RequiresApi
import nl.tudelft.trustchain.musicdao.core.ipv8.MusicCommunity
import nl.tudelft.trustchain.musicdao.core.ipv8.blocks.artistAnnounce.ArtistAnnounceBlock
import nl.tudelft.trustchain.musicdao.core.ipv8.blocks.artistAnnounce.ArtistAnnounceBlockRepository
import nl.tudelft.trustchain.musicdao.core.repositories.model.Album
import nl.tudelft.trustchain.musicdao.core.repositories.model.Artist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@RequiresApi(Build.VERSION_CODES.O)
@Singleton
class ArtistRepository @Inject constructor(
    private val artistAnnounceBlockRepository: ArtistAnnounceBlockRepository,
    private val albumRepository: AlbumRepository,
    private val musicCommunity: MusicCommunity
) {

    val stateFlows: MutableMap<String, MutableStateFlow<Artist?>> = mutableMapOf()
    suspend fun getArtist(publicKey: String): Artist? {
        return artistAnnounceBlockRepository.getOrCrawl(publicKey)?.let { toArtist(it) }
    }

    fun getArtists(): List<Artist> {
        return artistAnnounceBlockRepository.getAllLocal().map { toArtist(it) }
    }

    suspend fun getArtistReleases(publicKey: String): List<Album> {
        return albumRepository.getAlbumsFromArtist(publicKey = publicKey)
    }

    suspend fun getArtistStateFlow(publicKey: String): StateFlow<Artist?> {
        val current = stateFlows.get(publicKey)
        if (current == null) {
            val stateFlow = MutableStateFlow<Artist?>(null)
            stateFlow.value = getArtist(publicKey)
            stateFlows.set(publicKey, stateFlow)
            return stateFlow
        }
        return current
    }

    suspend fun refreshArtistStateFlow(publicKey: String) {
        val current = stateFlows.get(publicKey)

        if (current != null) {
            current.value = getArtist(publicKey)
        } else {
            stateFlows.set(publicKey, MutableStateFlow(getArtist(publicKey)))
        }
    }

    suspend fun getMyself(): Artist? {
        val publicKey = musicCommunity.publicKeyHex()
        return getArtist(publicKey)
    }

    suspend fun edit(
        name: String,
        bitcoinAddress: String,
        socials: String,
        biography: String
    ): Boolean {
        val result = artistAnnounceBlockRepository.create(
            ArtistAnnounceBlockRepository.Companion.Create(
                bitcoinAddress = bitcoinAddress,
                name = name,
                socials = socials,
                biography = biography
            )
        ) != null

        if (result) {
            refreshArtistStateFlow(musicCommunity.publicKeyHex())
        }
        return result
    }

    private fun toArtist(block: ArtistAnnounceBlock): Artist {
        return Artist(
            publicKey = block.publicKey,
            bitcoinAddress = block.bitcoinAddress,
            name = block.name,
            biography = block.biography,
            socials = block.socials,
            releaseIds = listOf()
        )
    }
}
