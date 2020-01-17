package nl.tudelft.ipv8.messaging

/**
 * Handler for messages coming in through an Endpoint.
 */
interface EndpointListener {
    /**
     * Callback for when data is received on this endpoint.
     *
     * @param packet The received packet.
     */
    fun onPacket(packet: Packet)
}
