package nl.tudelft.ipv8

import android.os.Handler
import nl.tudelft.ipv8.messaging.Endpoint
import nl.tudelft.ipv8.peerdiscovery.DiscoveryStrategy
import kotlin.math.roundToLong

class Ipv8(
    private val endpoint: Endpoint,
    private val configuration: Ipv8Configuration
) {
    private val overlayLock = Object()

    private val overlays = mutableListOf<Overlay>()
    private val strategies = mutableListOf<DiscoveryStrategy>()

    private val handler = Handler()
    private var nextTickRunnable: Runnable? = null

    fun start() {
        endpoint.open()

        // Init overlays and discovery strategies
        for (overlayConfiguration in configuration.overlays) {
            val overlay = overlayConfiguration.overlay
            overlays.add(overlay)
            overlay.load()
            for (strategy in overlayConfiguration.walkers) {
                strategies.add(strategy)
            }
        }

        // Start looping call
        scheduleNextTick()
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
                scheduleNextTick()
            }
        }
    }

    private fun scheduleNextTick() {
        val interval = (configuration.walkerInterval * 1000).roundToLong()
        val runnable = Runnable {
            onTick()
        }
        nextTickRunnable = runnable
        handler.postDelayed(runnable, interval)
    }

    fun stop() {
        synchronized(overlayLock) {
            for (overlay in overlays) {
                overlay.unload()
            }
            nextTickRunnable?.let {
                handler.removeCallbacks(it)
            }
            endpoint.close()
        }
    }
}
