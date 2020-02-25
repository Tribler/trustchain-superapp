package nl.tudelft.ipv8.peerdiscovery.strategy

import nl.tudelft.ipv8.Overlay

/**
 * Strategy for discovering peers in a network.
 */
interface DiscoveryStrategy {
    /**
     * It is called when the IPv8 service is started.
     */
    fun load() {
        // NOOP
    }

    /**
     * It is called on every tick in interval defined by [IPv8Configuration.walkerInterval].
     */
    fun takeStep()

    /**
     * It is called when the IPv8 service is stopped. Should be used to cleanup any resources.
     */
    fun unload() {
        // NOOP
    }

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
