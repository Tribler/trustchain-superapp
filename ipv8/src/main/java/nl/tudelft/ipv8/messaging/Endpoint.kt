package nl.tudelft.ipv8.messaging

import mu.KotlinLogging
import nl.tudelft.ipv8.Address

private val logger = KotlinLogging.logger {}

abstract class Endpoint {
    private val listeners = mutableListOf<EndpointListener>()

    fun addListener(listener: EndpointListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: EndpointListener) {
        listeners.remove(listener)
    }

    protected fun notifyListeners(packet: Packet) {
        for (listener in listeners) {
            try {
                listener.onPacket(packet)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    protected fun setEstimatedLan(address: Address) {
        logger.info("Estimated LAN address: $address")
        for (listener in listeners) {
            listener.onEstimatedLanChanged(address)
        }
    }

    abstract fun isOpen(): Boolean
    abstract fun send(address: Address, data: ByteArray)
    abstract fun open()
    abstract fun close()
}
