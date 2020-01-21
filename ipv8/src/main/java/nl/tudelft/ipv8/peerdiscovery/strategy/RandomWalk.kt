package nl.tudelft.ipv8.peerdiscovery.strategy

import android.util.Log
import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.Overlay
import java.util.*
import kotlin.random.Random

/**
 * Walk randomly through the network. On every step, request a random peer from the community for
 * an introduction to another community member.
 */
class RandomWalk(
    /**
     * The Overlay to walk over.
     */
    private val overlay: Overlay,

    /**
     * The timeout (in seconds) after which peers are considered unreachable.
     */
    private val timeout: Double = 3.0,

    /**
     * The amount of unanswered packets we can have in-flight.
     */
    private val windowSize: Int = 5,

    /**
     * The chance (0-255) to go back to the tracker.
     */
    private val resetChance: Int = 50,

    /**
     * The target interval (in seconds) between steps or 0 to use the default interval.
     */
    private val targetInterval: Int = 0,

    /**
     * The target number of peers the walker should try to connect to.
     */
    private val peers: Int = 20
) : DiscoveryStrategy {
        private val walkLock = Object()

    private var lastStep: Date? = null
    private val introTimeouts = mutableMapOf<Address, Date>()

    /**
     * Walk to random walkable peer.
     */
    override fun takeStep() {
        Log.d("RandomWalk", "takeStep")

        synchronized(walkLock) {
            if (peers >= 0 && overlay.getPeers().size >= peers) return

            // Sanitize unreachable nodes
            val toRemove = mutableListOf<Address>()
            for ((address, introTime) in introTimeouts) {
                if (introTime.time + timeout * 1000 < Date().time) {
                    toRemove += address
                }
            }
            for (node in toRemove) {
                introTimeouts.remove(node)
                if (overlay.network.getVerifiedByAddress(node) == null) {
                    overlay.network.removeByAddress(node)
                }
            }

            // Slow down the walk if a target interval has been specified
            val lastStep = lastStep
            if (targetInterval > 0 && lastStep != null && lastStep.time +
                targetInterval * 1000 >= Date().time) return

            // If a valid window size is specified and we are waiting for at least this many pings
            if (windowSize > 0 && introTimeouts.size >= windowSize) return

            // Take step
            val known = overlay.getWalkableAddresses()
            val available = (known.toSet() - introTimeouts.keys.toSet()).toList()

            // We can get stuck in an infinite loop of unreachable peers if we never contact
            // the tracker again
            if (available.isNotEmpty() && Random.nextInt(255) >= resetChance) {
                val peer = available.random()
                overlay.walkTo(peer)
                introTimeouts[peer] = Date()
            } else {
                overlay.getNewIntroduction()
            }

            this.lastStep = Date()
        }
    }
}
