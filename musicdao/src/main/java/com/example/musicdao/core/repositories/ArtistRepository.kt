package com.example.musicdao.core.repositories

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.musicdao.core.ipv8.blocks.artist_announce.ArtistAnnounceBlock
import com.example.musicdao.core.ipv8.repositories.ArtistAnnounceBlockRepository
import com.example.musicdao.core.model.Artist
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
class ArtistRepository @Inject constructor(
    private val artistAnnounceBlockRepository: ArtistAnnounceBlockRepository
) {

    suspend fun getArtist(publicKey: String): Artist? {
        return artistAnnounceBlockRepository.getOrCrawl(publicKey)?.let { toArtist(it) }
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


