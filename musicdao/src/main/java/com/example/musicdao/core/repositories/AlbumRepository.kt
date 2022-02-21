package com.example.musicdao.core.repositories

import com.example.musicdao.core.database.CacheDatabase
import com.example.musicdao.core.database.entities.AlbumEntity
import com.example.musicdao.core.ipv8.MusicCommunity
import com.example.musicdao.core.ipv8.blocks.Constants
import com.example.musicdao.core.ipv8.blocks.ReleasePublishBlock
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import javax.inject.Inject

/**
 * CRUD for any operations on Albums
 */
class AlbumRepository @Inject constructor(
    private val musicCommunity: MusicCommunity,
    private val database: CacheDatabase
) {

    suspend fun refreshReleases() {
        val releaseBlocks =
            musicCommunity.database.getBlocksWithType(ReleasePublishBlock.BLOCK_TYPE)
                .filter { trustChainBlock -> validateTrustChainReleaseBlock(trustChainBlock) }
                .map { trustChainBlock -> trustChainBlockToRelease(trustChainBlock) }
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

    fun publishRelease(
        releaseId: String,
        magnet: String,
        title: String,
        artist: String,
        publisher: String,
        releaseDate: String,
        protocolVersion: String = Constants.PROTOCOL_VERSION
    ) {
        val myPeer = IPv8Android.getInstance().myPeer
        val transaction = mutableMapOf(
            "releaseId" to releaseId,
            "magnet" to magnet,
            "title" to title,
            "artist" to artist,
            "publisher" to publisher,
            "releaseDate" to releaseDate,
            "protocolVersion" to protocolVersion
        )
        musicCommunity.createProposalBlock(
            "publish_release",
            transaction,
            myPeer.publicKey.keyToBin()
        )
    }

    fun validateReleaseBlock(
        releaseId: String,
        magnet: String,
        title: String,
        artist: String,
        releaseDate: String,
        publisher: String,
    ): Boolean {
        return (releaseId.isNotEmpty() && magnet.isNotEmpty() && title.isNotEmpty() && artist.isNotEmpty() && releaseDate.isNotEmpty() &&
            publisher.isNotEmpty())
    }

    private fun trustChainBlockToRelease(block: TrustChainBlock): ReleasePublishBlock {
        val releaseId = block.transaction["releaseId"] as String
        val magnet = block.transaction["magnet"] as String
        val title = block.transaction["title"] as String
        val artist = block.transaction["artist"] as String
        val publisher = block.transaction["publisher"] as String
        val releaseDate = block.transaction["releaseDate"] as String
        val protocolVersion = block.transaction["protocolVersion"] as String

        return ReleasePublishBlock(
            releaseId = releaseId,
            magnet = magnet,
            title = title,
            artist = artist,
            publisher = publisher,
            releaseDate = releaseDate,
            protocolVersion = protocolVersion
        )
    }

    private fun validateTrustChainReleaseBlock(block: TrustChainBlock): Boolean {
        val releaseId = block.transaction["releaseId"]
        val magnet = block.transaction["magnet"]
        val title = block.transaction["title"]
        val artist = block.transaction["artist"]
        val publisher = block.transaction["publisher"]
        val releaseDate = block.transaction["releaseDate"]
        val protocolVersion = block.transaction["protocolVersion"]

        return (releaseId is String && releaseId.isNotEmpty() &&
            magnet is String && magnet.isNotEmpty() &&
            title is String && title.isNotEmpty() &&
            artist is String && artist.isNotEmpty() &&
            publisher is String && publisher.isNotEmpty() &&
            releaseDate is String && releaseDate.isNotEmpty() &&
            protocolVersion is String && protocolVersion.isNotEmpty() && protocolVersion == Constants.PROTOCOL_VERSION)
    }
}
