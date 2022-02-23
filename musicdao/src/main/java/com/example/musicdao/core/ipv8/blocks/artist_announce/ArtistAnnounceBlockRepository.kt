package com.example.musicdao.core.ipv8.repositories

import android.util.Log
import com.example.musicdao.core.ipv8.MusicCommunity
import com.example.musicdao.core.ipv8.blocks.Constants
import com.example.musicdao.core.ipv8.blocks.artist_announce.ArtistAnnounceBlock
import com.example.musicdao.core.ipv8.blocks.artist_announce.ArtistAnnounceBlockValidator
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.toHex
import javax.inject.Inject

class ArtistAnnounceBlockRepository @Inject constructor(
    private val musicCommunity: MusicCommunity,
    private val artistAnnounceBlockValidator: ArtistAnnounceBlockValidator,
) {

    /**
     * Attempt to get a artist announce block.
     * 1. Search locally first.
     * 2. Try to query artist peer.
     * 3. Crawl around for artist.
     */
    suspend fun getOrCrawl(publicKey: String): ArtistAnnounceBlock? {
        val block = get(publicKey)
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

    private suspend fun crawl(publicKey: String) {
        val key = publicKey.toByteArray()
        val peer = musicCommunity.network.getVerifiedByPublicKeyBin(key)

        if (peer != null) {
            musicCommunity.crawlChain(peer = peer)
        } else {
            val randomPeers = musicCommunity.network.getRandomPeers(10) - musicCommunity.myPeer
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
            val socials: String,
        )
    }

}

