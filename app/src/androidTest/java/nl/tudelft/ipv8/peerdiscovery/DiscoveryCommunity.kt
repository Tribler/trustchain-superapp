package nl.tudelft.ipv8.peerdiscovery

import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.Endpoint

class DiscoveryCommunity(
    myPeer: Peer,
    endpoint: Endpoint,
    network: Network
) : Community(myPeer, endpoint, network) {
    override val serviceId = "7e313685c1912a141279f8248fc8db5899c5df5a"
}