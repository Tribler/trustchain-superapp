package nl.tudelft.ipv8.android.demo

import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainTransaction
import nl.tudelft.ipv8.attestation.trustchain.store.UserInfo
import nl.tudelft.ipv8.keyvault.CryptoProvider
import nl.tudelft.ipv8.messaging.Endpoint
import nl.tudelft.ipv8.peerdiscovery.Network

class DemoCommunity(
    myPeer: Peer,
    endpoint: Endpoint,
    network: Network,
    cryptoProvider: CryptoProvider,
    val trustChainCommunity: TrustChainCommunity
) : Community(myPeer, endpoint, network, 20, cryptoProvider) {
    override val serviceId = "02313685c1912a141279f8248fc8db5899c5df5a"

    fun getUsers(): List<UserInfo> {
        return trustChainCommunity.database.getUsers()
    }

    fun getPeerByPublicKeyBin(publicKeyBin: ByteArray): Peer? {
        return network.getVerifiedByPublicKeyBin(publicKeyBin)
    }

    fun crawlChain(peer: Peer, latestBlockNum: Int) {
        trustChainCommunity.crawlChain(peer, latestBlockNum)
    }

    /**
     * Creates a new proposal block, using a text message as the transaction content.
     */
    fun createProposalBlock(message: String, publicKey: ByteArray) {
        val blockType = "demo_block"
        val transaction = mapOf("message" to message)
        trustChainCommunity.createProposalBlock(blockType, transaction, publicKey)
    }

    fun getChainByUser(publicKeyBin: ByteArray): List<TrustChainBlock> {
        return trustChainCommunity.database.getLatestBlocks(publicKeyBin, 1000)
    }
}
