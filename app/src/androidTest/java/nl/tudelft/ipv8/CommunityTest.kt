package nl.tudelft.ipv8

import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import nl.tudelft.ipv8.keyvault.*
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.udp.UdpEndpoint
import nl.tudelft.ipv8.peerdiscovery.Network
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import org.junit.Assert
import org.junit.Test
import java.net.InetAddress

class CommunityTest {
    private fun getPrivateKey(): PrivateKey {
        val privateKey = "81df0af4c88f274d5228abb894a68906f9e04c902a09c68b9278bf2c7597eaf6"
        val signSeed = "c5c416509d7d262bddfcef421fc5135e0d2bdeb3cb36ae5d0b50321d766f19f2"
        return LibNaClSK(privateKey.hexToBytes(), signSeed.hexToBytes())
    }

    @Test
    fun onPacket() {
        val myPrivateKey = getPrivateKey()
        val myPeer = Peer(myPrivateKey)
        val endpoint = UdpEndpoint(0, InetAddress.getLocalHost())
        val network = Network()

        val community = TestCommunity(myPeer, endpoint, network)
        val handler = mockk<(Address, ByteArray) -> Unit>(relaxed = true)
        community.messageHandlers[246] = handler

        val packet = Packet(myPeer.address, "000260793bdb9cc0b60c96f88069d78aee327a6241d2f6004a4c69624e61434c504b3a7dc013cef4be5e4e051616a9b3cd9c8d8eb5192f037f3104f6323e43d83a934161ef4f7fe7ea4443da306cd998f830cf8bd543525afd929c83d641c7e9ba0ed300000000000000010102030404d20202030408ba030203040ca2010001e3e4862ec5a53c8e44e8bdeffbc8eb21cc441dbe90cc7018d4eb9183bf48d564cd86fe1d5c5d8d2298f7e2b746633ad995e2015597bfa53fb86fb70d1679a104".hexToBytes())
        community.onPacket(packet)

        verify { handler(myPeer.address, any()) }
    }

    @Test
    fun createIntroductionRequest() {
        val myPrivateKey = getPrivateKey()
        val myPeer = Peer(myPrivateKey)
        val endpoint = UdpEndpoint(0, InetAddress.getLocalHost())
        val network = Network()

        val community = spyk(TestCommunity(myPeer, endpoint, network))

        community.myEstimatedLan = Address("2.2.3.4", 2234)
        community.myEstimatedWan = Address("3.2.3.4", 3234)
        val packet = community.createIntroductionRequest(
            Address("1.2.3.4", 1234)
        )
        Assert.assertEquals("000260793bdb9cc0b60c96f88069d78aee327a6241d2f6004a4c69624e61434c504b3a7dc013cef4be5e4e051616a9b3cd9c8d8eb5192f037f3104f6323e43d83a934161ef4f7fe7ea4443da306cd998f830cf8bd543525afd929c83d641c7e9ba0ed300000000000000010102030404d20202030408ba030203040ca2010001e3e4862ec5a53c8e44e8bdeffbc8eb21cc441dbe90cc7018d4eb9183bf48d564cd86fe1d5c5d8d2298f7e2b746633ad995e2015597bfa53fb86fb70d1679a104", packet.toHex())
    }

    @Test
    fun createIntroductionResponse() {
        val myPrivateKey = getPrivateKey()

        val myPeer = Peer(myPrivateKey)
        val endpoint = UdpEndpoint(0, InetAddress.getLocalHost())
        val network = Network()

        val community = TestCommunity(myPeer, endpoint, network)

        community.myEstimatedLan = Address("2.2.3.4", 2234)
        community.myEstimatedWan = Address("3.2.3.4", 3234)
        val packet = community.createIntroductionResponse(
            Address("1.2.3.4", 1234),
            Address("1.2.3.4", 1234),
            2,
            introduction = Peer(ECCrypto.generateKey(), Address("5.2.3.4", 5234))
        )

        Assert.assertEquals("000260793bdb9cc0b60c96f88069d78aee327a6241d2f5004a4c69624e61434c504b3a7dc013cef4be5e4e051616a9b3cd9c8d8eb5192f037f3104f6323e43d83a934161ef4f7fe7ea4443da306cd998f830cf8bd543525afd929c83d641c7e9ba0ed300000000000000010102030404d20202030408ba030203040ca200000000000005020304147200000294b144e819f1b3179e8d6117ea7cf7846b4490273999f1bfd2e3c498f9486183be2464cb543003475c5cbff8458ce9e40170d332fad063e238d1dab448098801", packet.toHex())
    }
}
