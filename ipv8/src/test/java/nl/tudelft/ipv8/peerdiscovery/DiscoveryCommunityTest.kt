package nl.tudelft.ipv8.peerdiscovery

import com.goterl.lazycode.lazysodium.LazySodiumJava
import com.goterl.lazycode.lazysodium.SodiumJava
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import nl.tudelft.ipv8.Address
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.JavaCryptoProvider
import nl.tudelft.ipv8.keyvault.LibNaClSK
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.payload.ConnectionType
import nl.tudelft.ipv8.messaging.payload.GlobalTimeDistributionPayload
import nl.tudelft.ipv8.messaging.payload.IntroductionResponsePayload
import nl.tudelft.ipv8.messaging.udp.UdpEndpoint
import nl.tudelft.ipv8.util.hexToBytes
import org.junit.Assert.assertEquals
import org.junit.Test

private val lazySodium = LazySodiumJava(SodiumJava())

class DiscoveryCommunityTest {
    private fun getPrivateKey(): PrivateKey {
        val privateKey = "81df0af4c88f274d5228abb894a68906f9e04c902a09c68b9278bf2c7597eaf6"
        val signSeed = "c5c416509d7d262bddfcef421fc5135e0d2bdeb3cb36ae5d0b50321d766f19f2"
        return LibNaClSK(privateKey.hexToBytes(), signSeed.hexToBytes(), lazySodium)
    }

    private fun getEndpoint(): UdpEndpoint {
        return mockk(relaxed = true)
    }

    private fun createCommunity(): DiscoveryCommunity {
        val myPrivateKey = getPrivateKey()
        val myPeer = Peer(myPrivateKey)
        val endpoint = getEndpoint()
        val network = Network()
        return DiscoveryCommunity(myPeer, endpoint, network, JavaCryptoProvider)
    }

    @Test
    fun createSimilarityRequest() {
        val myPrivateKey = getPrivateKey()
        val myPeer = Peer(myPrivateKey)
        val endpoint = getEndpoint()
        val network = Network()
        val community = spyk(DiscoveryCommunity(myPeer, endpoint, network, JavaCryptoProvider))
        network.registerServiceProvider(community.serviceId, community)
        val peer = Peer(JavaCryptoProvider.generateKey(), Address("5.2.3.4", 5234))
        val payload = community.createSimilarityRequest(peer)
        community.handleSimilarityRequest(Packet(Address("1.2.3.4", 1234), payload))
        verify { community.onSimilarityRequest(any(), any()) }
    }

    @Test
    fun sendSimilarityRequest() {
        val myPrivateKey = getPrivateKey()
        val myPeer = Peer(myPrivateKey)
        val endpoint = getEndpoint()
        val network = Network()
        val community = spyk(DiscoveryCommunity(myPeer, endpoint, network, JavaCryptoProvider))
        network.registerServiceProvider(community.serviceId, community)
        val peer = Peer(JavaCryptoProvider.generateKey(), Address("5.2.3.4", 5234))
        community.sendSimilarityRequest(peer.address)
        verify { community.createSimilarityRequest(any()) }
    }

    @Test
    fun createSimilarityResponse() {
        val community = spyk(createCommunity())
        val peer = Peer(JavaCryptoProvider.generateKey(), Address("5.2.3.4", 5234))
        val payload = community.createSimilarityResponse(123, peer)
        community.handleSimilarityResponse(Packet(Address("1.2.3.4", 1234), payload))
        verify { community.onSimilarityResponse(any(), any()) }
    }

    @Test
    fun createPing() {
        val community = spyk(createCommunity())
        val (_, payload) = community.createPing()
        community.handlePing(Packet(Address("1.2.3.4", 1234), payload))
        verify { community.onPing(any(), any(), any()) }
    }

    @Test
    fun sendPing() {
        val community = spyk(createCommunity())
        val peer = Peer(JavaCryptoProvider.generateKey(), Address("5.2.3.4", 5234))
        community.sendPing(peer)
    }

    @Test
    fun createPong() {
        val community = spyk(createCommunity())
        val payload = community.createPong(123)
        community.handlePong(Packet(Address("1.2.3.4", 1234), payload))
        verify { community.onPong(any(), any()) }
    }

    @Test
    fun pingPong() {
        val community = spyk(createCommunity())
        val peer = Peer(JavaCryptoProvider.generateKey(), Address("5.2.3.4", 5234))
        community.sendPing(peer)

        val payload = community.createPong(1)
        community.handlePong(Packet(Address("1.2.3.4", 1234), payload))

        assertEquals(1, peer.pings.size)
    }

    @Test
    fun onIntroductionResponse_sendSimilarityRequest() {
        val community = spyk(createCommunity())
        val peer = Peer(JavaCryptoProvider.generateKey(), Address("5.2.3.4", 5234))
        val dist = GlobalTimeDistributionPayload(1u)
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
        community.onIntroductionResponse(peer, dist, payload)
        verify { community.sendSimilarityRequest(peer.address) }
    }

    @Test
    fun onSimilarityResponse_maxPeers() {
        val myPrivateKey = getPrivateKey()
        val myPeer = Peer(myPrivateKey)
        val endpoint = getEndpoint()
        val network = Network()
        network.addVerifiedPeer(Peer(JavaCryptoProvider.generateKey(), Address("12.2.3.4", 5234)))
        val community = DiscoveryCommunity(myPeer, endpoint, network, JavaCryptoProvider, 1)

        assertEquals(0, community.getPeers().size)
        assertEquals(1, network.verifiedPeers.size)

        val peer1 = Peer(JavaCryptoProvider.generateKey(), Address("13.2.3.4", 5234))
        val network1 = Network()
        val community1 = DiscoveryCommunity(peer1, endpoint, network1, JavaCryptoProvider, 2)
        network1.registerServiceProvider(community1.serviceId, community1)
        val payload1 = community1.createSimilarityResponse(123, peer1)
        community.handleSimilarityResponse(Packet(Address("13.2.3.4", 1234), payload1))
        assertEquals(1, community.getPeers().size)
        assertEquals(2, network.verifiedPeers.size)

        val peer2 = Peer(JavaCryptoProvider.generateKey(), Address("14.2.3.4", 5234))
        val network2 = Network()
        val community2 = DiscoveryCommunity(peer2, endpoint, network2, JavaCryptoProvider, 2)
        network2.registerServiceProvider(community2.serviceId, community2)
        val payload2 = community2.createSimilarityResponse(123, peer2)
        community.handleSimilarityResponse(Packet(Address("14.2.3.4", 5234), payload2))

        assertEquals(1, community.getPeers().size)
        assertEquals(2, network.verifiedPeers.size)
    }
}
