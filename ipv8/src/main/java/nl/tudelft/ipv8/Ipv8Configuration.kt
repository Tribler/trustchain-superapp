package nl.tudelft.ipv8

import nl.tudelft.ipv8.peerdiscovery.strategy.DiscoveryStrategy

class Ipv8Configuration(
    val address: String = "0.0.0.0",
    val port: Int = 8090,
    val walkerInterval: Double = 0.5,
    val overlays: List<OverlayConfiguration<*>>
)

class OverlayConfiguration<T : Overlay>(
    val overlay: T,
    val walkers: List<DiscoveryStrategy>
)
