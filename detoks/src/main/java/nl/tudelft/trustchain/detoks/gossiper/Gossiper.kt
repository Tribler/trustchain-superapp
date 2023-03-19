package nl.tudelft.trustchain.detoks.gossiper

import kotlinx.coroutines.CoroutineScope
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.util.random
import nl.tudelft.trustchain.detoks.DeToksCommunity

abstract class Gossiper() {

    /**
     * Interval between gossips.
     */
    abstract val delay: Long

    /**
     * Amount of items to gossip.
     */
    abstract val blocks: Int

    /**
     * Number of peers to gossip with.
     */
    abstract val peers: Int


    /**
     * Instantiates the gossiping process.
     */
    abstract fun startGossip(coroutineScope: CoroutineScope)

    /**
     * The gossip routine.
     */
    abstract suspend fun gossip()

    /**
     * Returns a random peer from the peers in the community.
     */
    protected fun pickRandomPeers(deToksCommunity: DeToksCommunity, n : Int): Collection<Peer> {
        val peers = deToksCommunity.getPeers()
        val maxPeers = if(peers.size < n) peers.size else n
        return peers.random(maxPeers)
    }
}
