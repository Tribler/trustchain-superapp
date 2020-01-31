package nl.tudelft.ipv8

import kotlinx.coroutines.*
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.messaging.Endpoint
import nl.tudelft.ipv8.peerdiscovery.strategy.DiscoveryStrategy
import java.lang.IllegalStateException
import kotlin.math.roundToLong

class Ipv8(
    private val endpoint: Endpoint,
    private val configuration: Ipv8Configuration
) {
    private val overlayLock = Object()

    private val overlays = mutableListOf<Overlay>()
    private val strategies = mutableListOf<DiscoveryStrategy>()

    private val scope = CoroutineScope(Dispatchers.IO)

    private var started = false

    fun getOverlays(): List<Overlay> {
        return overlays
    }

    fun start() {
        if (started) throw IllegalStateException("IPv8 has already started")

        started = true

        // Init overlays and discovery strategies
        for (overlayConfiguration in configuration.overlays) {
            val overlay = overlayConfiguration.overlay
            overlays.add(overlay)
            overlay.load()
            for (strategy in overlayConfiguration.walkers) {
                strategies.add(strategy)
            }
        }

        endpoint.open()

        // Start looping call
        startLoopingCall()
    }

    private fun onTick() {
        if (endpoint.isOpen()) {
            synchronized(overlayLock) {
                for (strategy in strategies) {
                    try {
                        // Strategies are prone to programmer error
                        strategy.takeStep()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun startLoopingCall() {
        val interval = (configuration.walkerInterval * 1000).roundToLong()
        scope.launch {
            while (true) {
                onTick()
                delay(interval)
            }
        }
    }

    fun stop() {
        if (!started) throw IllegalStateException("IPv8 is not running")

        synchronized(overlayLock) {
            scope.cancel()

            for (overlay in overlays) {
                overlay.unload()
            }

            endpoint.close()
        }

        started = false
    }
}
