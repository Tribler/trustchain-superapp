package nl.tudelft.ipv8

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import nl.tudelft.ipv8.keyvault.LibNaClSK
import nl.tudelft.ipv8.messaging.udp.UdpEndpoint
import nl.tudelft.ipv8.peerdiscovery.DiscoveryCommunity
import nl.tudelft.ipv8.peerdiscovery.Network
import nl.tudelft.ipv8.peerdiscovery.RandomChurn
import nl.tudelft.ipv8.peerdiscovery.RandomWalk
import java.net.InetAddress

class MainActivity : AppCompatActivity() {

    private var ipv8: Ipv8? = null

    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        resources.configuration.orientation

        // General community
        val myKey = LibNaClSK.generate()
        val address = Address("0.0.0.0", 8090)
        val myPeer = Peer(myKey, address, false)
        val endpoint = UdpEndpoint(8090, InetAddress.getByName("0.0.0.0"))
        val network = Network()
        val community = DiscoveryCommunity(myPeer, endpoint, network)
        val randomWalk = RandomWalk(community, timeout = 3.0)
        val randomChurn = RandomChurn(community)
        val overlayConfig = OverlayConfiguration(community, listOf(randomWalk, randomChurn))

        val config = Ipv8Configuration(overlays = listOf(overlayConfig), walkerInterval = 5.0)
        ipv8 = Ipv8(endpoint, config)
        ipv8?.start()

        loadNetworkInfo(network, community.serviceId)
    }

    override fun onDestroy() {
        ipv8?.stop()
        super.onDestroy()
    }

    private fun loadNetworkInfo(network: Network, serviceId: String) {
        handler.postDelayed({
            val peers = network.getPeersForService(serviceId)
            Log.d("MainActivity", "Found ${peers.size} peers")
            for (peer in peers) {
                Log.d("MainActivity", "$peer")
            }
            loadNetworkInfo(network, serviceId)
        }, 5000)
    }
}
