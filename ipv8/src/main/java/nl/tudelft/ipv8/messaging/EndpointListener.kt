package nl.tudelft.ipv8.messaging

import nl.tudelft.ipv8.Address

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

    /**
     * Callback for when the LAN address of the active network interface changes.
     *
     * @param packet The local LAN address.
     */
    fun onEstimatedLanChanged(address: Address)
}
