package nl.tudelft.ipv8.peerdiscovery

import nl.tudelft.ipv8.Overlay

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
    /**
     * Walk to random walkable peer.
     */
    override fun takeStep() {
        // TODO: implement
    }
}
