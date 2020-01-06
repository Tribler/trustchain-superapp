package nl.tudelft.ipv8.messaging

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
            listener.onPacket(packet)
        }
    }

    abstract fun isOpen(): Boolean
    abstract fun send(socketAddress: SocketAddress, data: ByteArray)
    abstract fun open()
    abstract fun close()
}
