package nl.tudelft.trustchain.musicdao.core.ipv8

import nl.tudelft.trustchain.musicdao.core.ipv8.blocks.artistAnnounce.ArtistAnnounceBlock
import nl.tudelft.trustchain.musicdao.core.ipv8.blocks.artistAnnounce.ArtistAnnounceBlockValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.Peer
import javax.inject.Inject

class ArtistBlockGossiper @Inject constructor(
    private val musicCommunity: MusicCommunity,
    private val artistAnnounceBlockSigner: ArtistAnnounceBlockValidator
) {

    fun startGossip(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            while (coroutineScope.isActive) {
                gossip()
                delay(Config.DELAY)
            }
        }
    }

    private fun gossip() {
        val randomPeer = pickRandomPeer()
        val artistBlocks =
            musicCommunity.database.getBlocksWithType(ArtistAnnounceBlock.BLOCK_TYPE)
                .filter { artistAnnounceBlockSigner.validateTransaction(it.transaction) }
                .shuffled()
                .take(Config.BLOCKS)
        artistBlocks.forEach {
            musicCommunity.sendBlock(it, randomPeer)
        }
    }

    object Config {
        const val BLOCKS = 10
        const val DELAY = 10_000L
    }

    private fun pickRandomPeer(): Peer? {
        val peers = musicCommunity.getPeers()
        if (peers.isEmpty()) return null
        return peers.random()
    }
}
