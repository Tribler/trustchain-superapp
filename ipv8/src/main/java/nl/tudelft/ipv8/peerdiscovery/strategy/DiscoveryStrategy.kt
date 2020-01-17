package nl.tudelft.ipv8.peerdiscovery.strategy

/**
 * Strategy for discovering peers in a network.
 */
interface DiscoveryStrategy {
    fun takeStep()
}
