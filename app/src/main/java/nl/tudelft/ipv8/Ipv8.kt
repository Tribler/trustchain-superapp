package nl.tudelft.ipv8

import nl.tudelft.ipv8.messaging.Endpoint

class Ipv8(
    private val endpoint: Endpoint,
    private val configuration: Ipv8Configuration
) {
    private val overlayLock = Object()

    fun start() {
        endpoint.open()

        // TODO: load/generate keys
        // TODO: init discovery strategy and overlays
    }

    fun onTick() {
        if (endpoint.isOpen()) {
            synchronized(overlayLock) {
                // TODO: implement
            }
        }
    }

    fun stop() {
        synchronized(overlayLock) {
            endpoint.close()
        }
    }
}
