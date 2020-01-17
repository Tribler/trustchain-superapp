package nl.tudelft.ipv8.peerdiscovery

import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer

/**
 * An overlay that supports pinging other peers.
 */
interface PingOverlay : Overlay {
    /**
     * Sends a ping message to the specified peer.
     */
    fun sendPing(peer: Peer)
}
