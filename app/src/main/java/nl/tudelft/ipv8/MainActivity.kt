package nl.tudelft.ipv8

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import nl.tudelft.ipv8.messaging.udp.UdpEndpoint
import nl.tudelft.ipv8.peerdiscovery.RandomWalk
import java.net.Inet4Address
import java.net.InetAddress

class MainActivity : AppCompatActivity() {

    private var ipv8: Ipv8? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // General community
        val community = TestCommunity()
        val randomWalk = RandomWalk(community, timeout = 3.0)
        val overlayConfig = OverlayConfiguration(community, listOf(randomWalk))

        val config = Ipv8Configuration(overlays = listOf(overlayConfig))
        val endpoint = UdpEndpoint(config.port, InetAddress.getByName(config.address))
        ipv8 = Ipv8(endpoint, config)
        ipv8?.start()
    }

    override fun onDestroy() {
        ipv8?.stop()
        super.onDestroy()
    }
}
