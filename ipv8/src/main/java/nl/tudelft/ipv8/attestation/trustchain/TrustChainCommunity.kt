package nl.tudelft.ipv8.attestation.trustchain

import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.CryptoProvider
import nl.tudelft.ipv8.messaging.Endpoint
import nl.tudelft.ipv8.peerdiscovery.Network

class TrustChainCommunity(
    myPeer: Peer,
    endpoint: Endpoint,
    network: Network,
    maxPeers: Int,
    cryptoProvider: CryptoProvider
) : Community(myPeer, endpoint, network, maxPeers, cryptoProvider) {
    override val serviceId = "5ad767b05ae592a02488272ca2a86b847d4562e1"

    init {

    }

    object MessageId {
        const val HALF_BLOCK = 1
        const val CRAWL_REQUEST = 2
        const val CRAWL_RESPONSE = 3
        const val HALF_BLOCK_PAIR = 4
        const val HALF_BLOCK_BROADCAST = 5
        const val HALF_BLOCK_PAIR_BROADCAST = 6
        const val EMPTY_CRAWL_RESPONSE = 7
    }
}
