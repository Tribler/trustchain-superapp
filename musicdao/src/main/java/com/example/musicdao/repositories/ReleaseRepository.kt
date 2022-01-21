package com.example.musicdao.repositories

import com.example.musicdao.ipv8.MusicCommunity
import com.example.musicdao.util.Util
import com.frostwire.jlibtorrent.TorrentInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock

data class ReleaseBlock(
    val magnet: String,
    val torrentInfoName: String,
    val title: String,
    val artist: String,
    val publisher: String,
)

class ReleaseRepository(private val musicCommunity: MusicCommunity) {

    private val _releaseBlocks: MutableStateFlow<List<ReleaseBlock>> =
        MutableStateFlow(listOf())
    private var releaseBlocks: StateFlow<List<ReleaseBlock>> = _releaseBlocks

    fun getReleaseBlocks(): StateFlow<List<ReleaseBlock>> {
        return this.releaseBlocks
    }

    fun refreshReleases() {
        this._releaseBlocks.value = fetchReleases()

    }

    fun getReleaseBlock(releaseId: String): ReleaseBlock {
        return _releaseBlocks.value.find { it.torrentInfoName == releaseId }!!
    }

    fun publishRelease(
        magnet: String,
        title: String,
        artists: String,
        releaseDate: String = "",
        torrentInfoName: String
    ) {
        val myPeer = IPv8Android.getInstance().myPeer
        val transaction = mutableMapOf<String, String>(
            "magnet" to magnet,
            "title" to title,
            "artists" to artists,
            "date" to releaseDate,
            "torrentInfoName" to torrentInfoName
        )
//        val walletDir = context?.cacheDir
//        if (walletDir != null) {
//            val musicWallet = WalletService.getInstance(walletDir, (activity as MusicService))
//            transaction["publisher"] = musicWallet.publicKey()
//        }
        musicCommunity.createProposalBlock(
            "publish_release",
            transaction,
            myPeer.publicKey.keyToBin()
        )
    }

    fun validateReleaseBlock(
        title: String?,
        artist: String?,
        releaseDate: String?,
        magnet: String?,
        torrentInfo: TorrentInfo
    ): String? {
        return if (title == null || artist == null || releaseDate == null || magnet == null ||
            title.isEmpty() || artist.isEmpty() || releaseDate.isEmpty() || magnet.isEmpty()
        ) {
            null
        } else {
            if (torrentInfo == null) {
                // If we only have a magnet link, extract the name from it to use for the
                // .torrent
                Util.extractNameFromMagnet(magnet)
            } else {
                torrentInfo.name()
            }
        }
    }


    private fun fetchReleases(): List<ReleaseBlock> {
        val releaseBlocks = musicCommunity.database.getBlocksWithType("publish_release")

        return releaseBlocks
            .filter { trustChainBlock -> validateTrustChainReleaseBlock(trustChainBlock) }
            .map { trustChainBlock -> trustChainBlockToRelease(trustChainBlock) }
    }

    private fun trustChainBlockToRelease(block: TrustChainBlock): ReleaseBlock {
        val magnet = block.transaction["magnet"] as String
        val torrentInfoName = block.transaction["torrentInfoName"] as String
        val title = block.transaction["title"] as String
        val publisher = "dsad"
        val artist = block.transaction["artists"] as String

        return ReleaseBlock(magnet, torrentInfoName, title, artist, publisher)
    }

    private fun validateTrustChainReleaseBlock(block: TrustChainBlock): Boolean {
        val magnet = block.transaction["magnet"]
        val torrentInfoName = block.transaction["torrentInfoName"]
        val title = block.transaction["title"]

        return (magnet is String && magnet.length > 0 && title is String && title.length > 0 &&
            torrentInfoName is String && torrentInfoName.length > 0)
    }

}
