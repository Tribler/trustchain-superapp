package nl.tudelft.ipv8.messaging

interface EndpointListener {
    fun onPacket(packet: Packet)
}
