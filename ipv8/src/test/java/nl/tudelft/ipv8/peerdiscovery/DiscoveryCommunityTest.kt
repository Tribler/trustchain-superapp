package nl.tudelft.ipv8.peerdiscovery

import io.mockk.spyk
import io.mockk.verify
import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.BaseCommunityTest
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.JavaCryptoProvider
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.payload.ConnectionType
import nl.tudelft.ipv8.messaging.payload.IntroductionResponsePayload
import org.junit.Assert.assertEquals
import org.junit.Test

class DiscoveryCommunityTest : BaseCommunityTest() {
    private fun getCommunity(): DiscoveryCommunity {
        val myPrivateKey = getPrivateKey()
        val myPeer = Peer(myPrivateKey)
        val endpoint = getEndpoint()
        val network = Network()
        val community = DiscoveryCommunity()
        community.myPeer = myPeer
        community.endpoint = endpoint
        community.network = network
        return community
    }

    @Test
    fun createSimilarityRequest() {
        val community = spyk(getCommunity())
        community.network.registerServiceProvider(community.serviceId, community)
        val peer = Peer(JavaCryptoProvider.generateKey(), Address("5.2.3.4", 5234))
        val payload = community.createSimilarityRequest(peer)
        community.onSimilarityRequestPacket(Packet(Address("1.2.3.4", 1234), payload))
        verify { community.onSimilarityRequest(any(), any()) }
    }

    @Test
    fun sendSimilarityRequest() {
        val community = spyk(getCommunity())
        community.network.registerServiceProvider(community.serviceId, community)
        val peer = Peer(JavaCryptoProvider.generateKey(), Address("5.2.3.4", 5234))
        community.sendSimilarityRequest(peer.address)
        verify { community.createSimilarityRequest(any()) }
    }

    @Test
    fun createSimilarityResponse() {
        val community = spyk(getCommunity())
        val peer = Peer(JavaCryptoProvider.generateKey(), Address("5.2.3.4", 5234))
        val payload = community.createSimilarityResponse(123, peer)
        community.onSimilarityResponsePacket(Packet(Address("1.2.3.4", 1234), payload))
        verify { community.onSimilarityResponse(any(), any()) }
    }

    @Test
    fun createPing() {
        val community = spyk(getCommunity())
        val (_, payload) = community.createPing()
        community.onPingPacket(Packet(Address("1.2.3.4", 1234), payload))
        verify { community.onPing(any(), any()) }
    }

    @Test
    fun sendPing() {
        val community = spyk(getCommunity())
        val peer = Peer(JavaCryptoProvider.generateKey(), Address("5.2.3.4", 5234))
        community.sendPing(peer)
    }

    @Test
    fun createPong() {
        val community = spyk(getCommunity())
        val payload = community.createPong(123)
        community.onPongPacket(Packet(Address("1.2.3.4", 1234), payload))
        verify { community.onPong(any()) }
    }

    @Test
    fun pingPong() {
        val community = spyk(getCommunity())
        val peer = Peer(JavaCryptoProvider.generateKey(), Address("5.2.3.4", 5234))
        community.sendPing(peer)

        val payload = community.createPong(1)
        community.onPongPacket(Packet(Address("1.2.3.4", 1234), payload))

        assertEquals(1, peer.pings.size)
    }

    @Test
    fun onIntroductionResponse_sendSimilarityRequest() {
        val community = spyk(getCommunity())
        val peer = Peer(JavaCryptoProvider.generateKey(), Address("5.2.3.4", 5234))
        val payload = IntroductionResponsePayload(
            Address("1.2.3.4", 1234),
            Address("2.2.3.4", 2234),
            Address("3.2.3.4", 3234),
            Address("4.2.3.4", 4234),
            Address("5.2.3.4", 5234),
            ConnectionType.UNKNOWN,
            false,
            2
        )
        community.onIntroductionResponse(peer, payload)
        verify { community.sendSimilarityRequest(peer.address) }
    }

    @Test
    fun onSimilarityResponse_maxPeers() {
        val myPrivateKey = getPrivateKey()
        val myPeer = Peer(myPrivateKey)
        val endpoint = getEndpoint()
        val network = Network()
        network.addVerifiedPeer(Peer(JavaCryptoProvider.generateKey(), Address("12.2.3.4", 5234)))
        val community = DiscoveryCommunity()
        community.myPeer = myPeer
        community.endpoint = endpoint
        community.network = network
        community.maxPeers = 1

        assertEquals(0, community.getPeers().size)
        assertEquals(1, network.verifiedPeers.size)

        val peer1 = Peer(JavaCryptoProvider.generateKey(), Address("13.2.3.4", 5234))
        val network1 = Network()
        val community1 = DiscoveryCommunity()
        community1.myPeer = peer1
        community1.endpoint = endpoint
        community1.network = network1
        community1.maxPeers = 1
        network1.registerServiceProvider(community1.serviceId, community1)
        val payload1 = community1.createSimilarityResponse(123, peer1)
        community.onSimilarityResponsePacket(Packet(Address("13.2.3.4", 1234), payload1))
        assertEquals(1, community.getPeers().size)
        assertEquals(2, network.verifiedPeers.size)

        val peer2 = Peer(JavaCryptoProvider.generateKey(), Address("14.2.3.4", 5234))
        val network2 = Network()
        val community2 = DiscoveryCommunity()
        community2.myPeer = peer2
        community2.endpoint = endpoint
        community2.network = network2
        community2.maxPeers = 1
        network2.registerServiceProvider(community2.serviceId, community2)
        val payload2 = community2.createSimilarityResponse(123, peer2)
        community.onSimilarityResponsePacket(Packet(Address("14.2.3.4", 5234), payload2))

        assertEquals(1, community.getPeers().size)
        assertEquals(2, network.verifiedPeers.size)
    }
}
