package nl.tudelft.ipv8.tracker

import mu.KotlinLogging
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.peerdiscovery.strategy.DiscoveryStrategy
import java.util.*

private val logger = KotlinLogging.logger {}

class SimpleChurn(
    private val overlay: Overlay
) : DiscoveryStrategy {
    override fun takeStep() {
        synchronized(overlay.network.graphLock) {
            overlay.network.verifiedPeers.filter { peer ->
                Date().time - (peer.lastResponse?.time ?: 0L) > 120_000L
            }.forEach {
                overlay.network.removePeer(it)
                logger.debug { "Removed inactive peer $it" }
            }
        }
    }

    class Factory: DiscoveryStrategy.Factory<SimpleChurn>() {
        override fun create(): SimpleChurn {
            return SimpleChurn(getOverlay())
        }
    }
}
