package nl.tudelft.ipv8.peerdiscovery

import android.util.Log
import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.Peer
import java.util.*
import kotlin.math.min

class RandomChurn(
    /**
     * The Overlay to sample peers from.
     */
    private val overlay: DiscoveryCommunity,

    /**
     * The amount of peers to check at once.
     */
    private val sampleSize: Int = 8,

    /**
     * Time between pings in the range of [inactiveTime] to [dropTime].
     */
    private val pingInterval: Double = 10.0,

    /**
     * Time before pings are sent to check liveness.
     */
    private val inactiveTime: Double = 27.5,

    /**
     * Time after which a peer is dropped.
     */
    private val dropTime: Double = 57.5
) : DiscoveryStrategy {
    private val walkLock = Object()

    private val pinged = mutableMapOf<Address, Date>()

    /**
     * Have we passed the time before we consider this peer to be unreachable.
     */
    private fun shouldDrop(peer: Peer): Boolean {
        val lastResponse = peer.lastResponse ?: return false
        return Date().time > lastResponse.time + dropTime * 1000
    }

    /**
     * Have we passed the time before we consider this peer to be inactive.
     */
    private fun isInactive(peer: Peer): Boolean {
        val lastResponse = peer.lastResponse ?: return false
        return Date().time > lastResponse.time + inactiveTime * 1000
    }

    override fun takeStep() {
        synchronized(walkLock) {
            val sampleSize = min(overlay.network.verifiedPeers.size, sampleSize)
            if (sampleSize > 0) {
                val verified = overlay.network.verifiedPeers.shuffled()
                val window = verified.subList(0, sampleSize)

                for (peer in window) {
                    if (shouldDrop(peer) && pinged.contains(peer.address)) {
                        Log.d("RandomChurn", "Dropping inactive peer $peer")
                        overlay.network.removePeer(peer)
                        pinged.remove(peer.address)
                    } else if (isInactive(peer) || peer.pings.size < Peer.MAX_PINGS) {
                        Log.d("RandomChurn", "Peer $peer is inactive")
                        val pingedAddress = pinged[peer.address]
                        if (pingedAddress != null) {
                            if (Date().time > pingedAddress.time + pingInterval * 1000) {
                                pinged.remove(peer.address)
                            }
                        } else {
                            pinged[peer.address] = Date()
                            overlay.sendPing(peer)
                        }
                    }
                }
            }
        }
    }
}
