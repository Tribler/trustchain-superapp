package com.example.musicdao.repositories

import com.example.musicdao.ipv8.MusicCommunity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock

data class ReleaseBlock(
    val releaseId: String,
    val magnet: String,
    val title: String,
    val artist: String,
    val publisher: String,
    val releaseDate: String,
    val protocolVersion: String
)

class ReleaseRepository(private val musicCommunity: MusicCommunity) {

    val PROTOCOL_VERSION = "1"

    private val _releaseBlocks: MutableStateFlow<List<ReleaseBlock>> =
        MutableStateFlow(listOf())
    private var releaseBlocks: StateFlow<List<ReleaseBlock>> = _releaseBlocks

    fun getReleaseBlocks(): StateFlow<List<ReleaseBlock>> {
        return this.releaseBlocks
    }

    fun searchReleaseBlocksLocal(keyword: String): List<ReleaseBlock> {
        return this.releaseBlocks.value.filter {
            it.artist.lowercase().contains(keyword) || it.title.lowercase().contains(keyword)
        }
    }

    fun refreshReleases() {
        this._releaseBlocks.value = fetchReleases()
    }

    fun getReleaseBlock(releaseId: String): ReleaseBlock {
        return _releaseBlocks.value.find { it.releaseId == releaseId }!!
    }

    private fun fetchReleases(): List<ReleaseBlock> {
        val releaseBlocks = musicCommunity.database.getBlocksWithType("publish_release")

        return releaseBlocks
            .filter { trustChainBlock -> validateTrustChainReleaseBlock(trustChainBlock) }
            .map { trustChainBlock -> trustChainBlockToRelease(trustChainBlock) }
    }

    fun publishRelease(
        releaseId: String,
        magnet: String,
        title: String,
        artist: String,
        publisher: String,
        releaseDate: String,
        protocolVersion: String = PROTOCOL_VERSION
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

    private fun trustChainBlockToRelease(block: TrustChainBlock): ReleaseBlock {
        val releaseId = block.transaction["releaseId"] as String
        val magnet = block.transaction["magnet"] as String
        val title = block.transaction["title"] as String
        val artist = block.transaction["artist"] as String
        val publisher = block.transaction["publisher"] as String
        val releaseDate = block.transaction["releaseDate"] as String
        val protocolVersion = block.transaction["protocolVersion"] as String

        return ReleaseBlock(
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
            protocolVersion is String && protocolVersion.isNotEmpty() && protocolVersion == PROTOCOL_VERSION)
    }
}
