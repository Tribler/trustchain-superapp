package nl.tudelft.trustchain.musicdao.core.ipv8

import nl.tudelft.trustchain.musicdao.core.ipv8.blocks.releasePublish.ReleasePublishBlock
import nl.tudelft.trustchain.musicdao.core.ipv8.blocks.releasePublish.ReleasePublishBlockValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.Peer
import javax.inject.Inject

class ReleaseBlockGossiper @Inject constructor(
    private val musicCommunity: MusicCommunity,
    private val releasePublishBlockSigner: ReleasePublishBlockValidator
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
        val releaseBlocks =
            musicCommunity.database.getBlocksWithType(ReleasePublishBlock.BLOCK_TYPE)
                .filter { releasePublishBlockSigner.validateTransaction(it.transaction) }
                .shuffled()
                .take(Config.BLOCKS)
        releaseBlocks.forEach {
            musicCommunity.sendBlock(it, randomPeer)
        }
    }

    object Config {
        const val BLOCKS = 10
        const val DELAY = 5_000L
    }

    private fun pickRandomPeer(): Peer? {
        val peers = musicCommunity.getPeers()
        if (peers.isEmpty()) return null
        return peers.random()
    }
}
