package nl.tudelft.ipv8

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import nl.tudelft.ipv8.keyvault.Key
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.messaging.udp.UdpEndpoint
import nl.tudelft.ipv8.peerdiscovery.Network
import nl.tudelft.ipv8.util.toHex
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.net.InetAddress

@RunWith(AndroidJUnit4::class)
class CommunityTest {
    @Test
    fun createIntroductionRequest() {
        val myKey = mockk<Key>(relaxed = true)
        val myPublicKey = mockk<PublicKey>(relaxed = true)
        every { myKey.pub() } returns myPublicKey
        every { myPublicKey.keyToBin() } returns "aaa".toByteArray(Charsets.US_ASCII)

        val myPeer = Peer(myKey)
        val endpoint = UdpEndpoint(0, InetAddress.getLocalHost())
        val network = Network()

        val community = TestCommunity(myPeer, endpoint, network)

        community.myEstimatedLan = Address("2.2.3.4", 2234)
        community.myEstimatedWan = Address("3.2.3.4", 3234)
        val packet = community.createIntroductionRequest(
            Address("1.2.3.4", 1234)
        )
        Assert.assertEquals("3030303236303739336264623963633062363063393666383830363964373861656533323761363234316432f6000361616100000000000000010102030404d20202030408ba030203040ca2010001", packet.toHex())

        //community.handleIntroductionRequest(myPeer.address, packet)
    }

    @Test
    fun createIntroductionResponse() {
        val myKey = mockk<Key>(relaxed = true)
        val myPublicKey = mockk<PublicKey>(relaxed = true)
        every { myKey.pub() } returns myPublicKey
        every { myPublicKey.keyToBin() } returns "aaa".toByteArray(Charsets.US_ASCII)

        val myPeer = Peer(myKey)
        val endpoint = UdpEndpoint(0, InetAddress.getLocalHost())
        val network = Network()

        val community = TestCommunity(myPeer, endpoint, network)

        community.myEstimatedLan = Address("2.2.3.4", 2234)
        community.myEstimatedWan = Address("3.2.3.4", 3234)
        val packet = community.createIntroductionResponse(
            Address("1.2.3.4", 1234),
            Address("2.2.3.4", 1234),
            1
        )
    }
}
