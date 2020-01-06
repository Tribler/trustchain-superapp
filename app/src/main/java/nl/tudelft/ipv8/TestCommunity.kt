package nl.tudelft.ipv8

import nl.tudelft.ipv8.messaging.Endpoint
import nl.tudelft.ipv8.peerdiscovery.Network

class TestCommunity(
    myPeer: Peer,
    endpoint: Endpoint,
    network: Network
) : Community(myPeer, endpoint, network) {
    // TODO
    override val masterPeer: Peer = Peer()
}
