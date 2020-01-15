package nl.tudelft.ipv8

import nl.tudelft.ipv8.keyvault.LibNaClPK
import nl.tudelft.ipv8.messaging.Endpoint
import nl.tudelft.ipv8.peerdiscovery.Network
import nl.tudelft.ipv8.util.hexToBytes

class ExampleCommunity(
    myPeer: Peer,
    endpoint: Endpoint,
    network: Network
) : Community(myPeer, endpoint, network) {
    override val serviceId = Peer(LibNaClPK.fromBin(MASTER_PEER_KEY.hexToBytes())).mid

    companion object {
        private const val MASTER_PEER_KEY = "4c69624e61434c504b3ae30bbf2554d0d389964bfb3630eed1f8a216791afa48b335f04a499d6e87e14bdf53b02c329b6198312f252eddfb3119f038d71f3381092de82a83de0a0443df"
    }
}
