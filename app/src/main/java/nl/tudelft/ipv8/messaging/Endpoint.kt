package nl.tudelft.ipv8.messaging

import nl.tudelft.ipv8.Address
import java.net.SocketAddress

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

    abstract fun isOpen(): Boolean
    abstract fun send(address: Address, data: ByteArray)
    abstract fun open()
    abstract fun close()
}
