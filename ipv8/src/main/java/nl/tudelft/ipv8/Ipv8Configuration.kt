package nl.tudelft.ipv8

import nl.tudelft.ipv8.peerdiscovery.strategy.DiscoveryStrategy

class Ipv8Configuration(
    val address: String = "0.0.0.0",
    val port: Int = 8090,
    val walkerInterval: Double = 0.5,
    val overlays: List<OverlayConfiguration>
)

class OverlayConfiguration(
    val overlay: Overlay,
    val walkers: List<DiscoveryStrategy>
)
