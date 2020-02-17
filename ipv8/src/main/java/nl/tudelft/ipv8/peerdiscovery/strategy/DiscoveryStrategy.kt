package nl.tudelft.ipv8.peerdiscovery.strategy

import nl.tudelft.ipv8.Overlay

/**
 * Strategy for discovering peers in a network.
 */
interface DiscoveryStrategy {
    fun takeStep()

    abstract class Factory<T : DiscoveryStrategy> {
        private var overlay: Overlay? = null

        protected fun getOverlay(): Overlay {
            return overlay ?: throw IllegalStateException("Overlay is not set")
        }

        fun setOverlay(overlay: Overlay): Factory<T> {
            this.overlay = overlay
            return this
        }

        abstract fun create(): T
    }
}
