package nl.tudelft.trustchain.detoks

import kotlinx.coroutines.CoroutineScope
import nl.tudelft.ipv8.Peer

abstract class Gossiper() {

    /**
     * Interval between gossips.
     */
    abstract val delay: Long

    /**
     * Amount of items gossiped.
     */
    abstract val blocks: Int


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
    protected fun pickRandomPeer(deToksCommunity: DeToksCommunity): Peer? {
        val peers = deToksCommunity.getPeers()
        if (peers.isEmpty()) return null
        return peers.random()
    }
}
