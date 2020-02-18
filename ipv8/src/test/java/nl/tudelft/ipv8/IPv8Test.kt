package nl.tudelft.ipv8

import nl.tudelft.ipv8.keyvault.JavaCryptoProvider
import nl.tudelft.ipv8.messaging.udp.UdpEndpoint
import nl.tudelft.ipv8.peerdiscovery.strategy.RandomWalk
import org.junit.Assert
import org.junit.Test
import java.net.InetAddress

class IPv8Test {
    @Test
    fun startAndStop() {
        val myKey = JavaCryptoProvider.generateKey()
        val endpoint = UdpEndpoint(8090, InetAddress.getByName("0.0.0.0"))
        val randomWalk = RandomWalk.Factory(
            timeout = 3.0
        )
        val factory = Overlay.Factory(TestCommunity::class.java)
        val overlayConfig = OverlayConfiguration(
            factory,
            listOf(randomWalk)
        )
        val config = IPv8Configuration(
            overlays = listOf(overlayConfig),
            walkerInterval = 5.0
        )
        val ipv8 = IPv8(endpoint, config, myKey)
        ipv8.start()
        Assert.assertTrue(endpoint.isOpen())
        ipv8.stop()
    }
}
