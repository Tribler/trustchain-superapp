package nl.tudelft.trustchain.musicdao.core.ipv8.blocks.artistAnnounce

import android.annotation.SuppressLint
import android.util.Log
import nl.tudelft.trustchain.musicdao.core.ipv8.MusicCommunity
import nl.tudelft.trustchain.musicdao.core.ipv8.blocks.Constants
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.toHex
import javax.inject.Inject

class ArtistAnnounceBlockRepository @Inject constructor(
    private val musicCommunity: MusicCommunity,
    private val artistAnnounceBlockValidator: ArtistAnnounceBlockValidator
) {

    /**
     * Attempt to get a artist announce block.
     * 1. Search locally first.
     * 2. Try to query artist peer.
     * 3. Crawl around for artist.
     */
    suspend fun getOrCrawl(publicKey: String): ArtistAnnounceBlock? {
        val block = get(publicKey)
        Log.d("MusicDao", "getOrCrawl 1: $block $publicKey")
        return if (block != null) {
            block
        } else {
            crawl(publicKey)
            get(publicKey)
        }
    }

    /**
     * Try to get the latest information on the artist.
     */
    fun get(publicKey: String): ArtistAnnounceBlock? {
        val blocks = musicCommunity.database.getBlocksWithType(ArtistAnnounceBlock.BLOCK_TYPE)
            .filter { artistAnnounceBlockValidator.validateTransaction(it.transaction) }
            .filter { it.publicKey.toHex() == publicKey }
            .sortedByDescending { it.sequenceNumber }
            .take(1)

        return if (blocks.isNotEmpty()) {
            val mostUpdatedAnnounce = blocks[0]
            ArtistAnnounceBlock.fromTrustChainTransaction(mostUpdatedAnnounce.transaction)
        } else {
            null
        }
    }

    /**
     * Try to get the latest information on all locally known artists.
     */
    fun getAllLocal(): List<ArtistAnnounceBlock> {
        return musicCommunity.database.getBlocksWithType(ArtistAnnounceBlock.BLOCK_TYPE)
            .filter { artistAnnounceBlockValidator.validateTransaction(it.transaction) }
            .sortedByDescending { it.sequenceNumber }
            .distinctBy { it.publicKey.toHex() }
            .map { ArtistAnnounceBlock.fromTrustChainTransaction(it.transaction) }
    }

    @SuppressLint("NewApi")
    private suspend fun crawl(publicKey: String) {
        val key = musicCommunity.publicKeyStringToByteArray(publicKey)
        val peer = musicCommunity.network.getVerifiedByPublicKeyBin(key)
        Log.d("MusicDao", "crawl: peer is? $peer")
        Log.d("MusicDao", "all peers")
        musicCommunity.network.verifiedPeers.forEach {
            Log.d("MusicDao", "peer: ${it.publicKey.keyToBin().toHex()}")
        }

        if (peer != null) {
            Log.d("MusicDao", "crawl: peer found $peer, crawling")
            musicCommunity.crawlChain(peer = peer)
        } else {
            val randomPeers = musicCommunity.network.getRandomPeers(10) - musicCommunity.myPeer
            Log.d("MusicDao", "crawl: crawling random peers ${randomPeers.size}")
            try {
                randomPeers.forEach {
                    musicCommunity.sendCrawlRequest(it, key, LongRange(-1, -1))
                }
            } catch (e: Exception) {
                return
            }
        }
    }

    fun create(create: Create): TrustChainBlock? {
        val transaction = mutableMapOf(
            "publicKey" to musicCommunity.myPeer.publicKey.keyToBin().toHex(),
            "bitcoinAddress" to create.bitcoinAddress,
            "name" to create.name,
            "biography" to create.biography,
            "socials" to create.socials,
            "protocolVersion" to Constants.PROTOCOL_VERSION
        )

        if (!artistAnnounceBlockValidator.validateTransaction(transaction)) {
            return null
        }

        return musicCommunity.createProposalBlock(
            blockType = ArtistAnnounceBlock.BLOCK_TYPE,
            transaction = transaction,
            publicKey = musicCommunity.myPeer.publicKey.keyToBin()
        )
    }

    companion object {
        data class Create(
            val bitcoinAddress: String,
            val name: String,
            val biography: String,
            val socials: String
        )
    }
}
