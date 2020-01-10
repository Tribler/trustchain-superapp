package nl.tudelft.ipv8

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import nl.tudelft.ipv8.keyvault.LibNaClSK
import nl.tudelft.ipv8.messaging.udp.UdpEndpoint
import nl.tudelft.ipv8.peerdiscovery.Network
import nl.tudelft.ipv8.peerdiscovery.RandomWalk
import java.net.InetAddress

class MainActivity : AppCompatActivity() {

    private var ipv8: Ipv8? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // General community
        val myKey = LibNaClSK.generate()
        val address = Address("0.0.0.0", 8090)
        val myPeer = Peer(myKey, address, false)
        val endpoint = UdpEndpoint(8090, InetAddress.getByName("0.0.0.0"))
        val network = Network()
        val community = ExampleCommunity(myPeer, endpoint, network)
        val randomWalk = RandomWalk(community, timeout = 3.0)
        val overlayConfig = OverlayConfiguration(community, listOf(randomWalk))

        val config = Ipv8Configuration(overlays = listOf(overlayConfig))
        ipv8 = Ipv8(endpoint, config)
        ipv8?.start()
    }

    override fun onDestroy() {
        ipv8?.stop()
        super.onDestroy()
    }
}
