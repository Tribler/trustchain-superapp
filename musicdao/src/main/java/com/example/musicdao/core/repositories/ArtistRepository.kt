package com.example.musicdao.core.repositories

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.musicdao.core.ipv8.MusicCommunity
import com.example.musicdao.core.ipv8.blocks.artist_announce.ArtistAnnounceBlock
import com.example.musicdao.core.ipv8.repositories.ArtistAnnounceBlockRepository
import com.example.musicdao.core.repositories.model.Album
import com.example.musicdao.core.repositories.model.Artist
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
class ArtistRepository @Inject constructor(
    private val artistAnnounceBlockRepository: ArtistAnnounceBlockRepository,
    private val albumRepository: AlbumRepository,
    private val musicCommunity: MusicCommunity
) {

    suspend fun getArtist(publicKey: String): Artist? {
        return artistAnnounceBlockRepository.getOrCrawl(publicKey)?.let { toArtist(it) }
    }

    fun getArtists(): List<Artist> {
        return artistAnnounceBlockRepository.getAllLocal().map { toArtist(it) }
    }

    suspend fun getArtistReleases(publicKey: String): List<Album> {
        return albumRepository.getAlbumsFromArtist(publicKey = publicKey)
    }

    suspend fun getMyself(): Artist? {
        val publicKey = musicCommunity.publicKeyHex()
        return getArtist(publicKey)
    }

    fun edit(
        name: String,
        bitcoinAddress: String,
        socials: String,
        biography: String
    ): Boolean {
        return artistAnnounceBlockRepository.create(
            ArtistAnnounceBlockRepository.Companion.Create(
                bitcoinAddress = bitcoinAddress,
                name = name,
                socials = socials,
                biography = biography
            )
        ) != null
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
