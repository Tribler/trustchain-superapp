package nl.tudelft.ipv8.peerdiscovery

import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.Overlay
import java.util.*
import kotlin.random.Random

/**
 * Walk randomly through the network.
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
    private val targetInterval: Int = 0
) : DiscoveryStrategy {
    private val walkLock = Object()

    private var lastStep: Date? = null
    private val introTimeouts = mutableMapOf<Address, Date>()

    /**
     * Walk to random walkable peer.
     */
    override fun takeStep() {
        synchronized(walkLock) {
            // TODO: Sanitize unreachable nodes

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
