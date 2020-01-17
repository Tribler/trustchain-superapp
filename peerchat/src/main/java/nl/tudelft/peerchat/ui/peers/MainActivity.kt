package nl.tudelft.peerchat.ui.peers

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.activity_main.*
import nl.tudelft.ipv8.*
import nl.tudelft.ipv8.keyvault.LibNaClSK
import nl.tudelft.ipv8.messaging.udp.UdpEndpoint
import nl.tudelft.ipv8.peerdiscovery.DiscoveryCommunity
import nl.tudelft.ipv8.peerdiscovery.Network
import nl.tudelft.ipv8.peerdiscovery.strategy.PeriodicSimilarity
import nl.tudelft.ipv8.peerdiscovery.strategy.RandomChurn
import nl.tudelft.ipv8.peerdiscovery.strategy.RandomWalk
import nl.tudelft.peerchat.R
import java.net.InetAddress

class MainActivity : AppCompatActivity() {

    private var ipv8: Ipv8? = null

    private val handler = Handler()

    private val adapter = ItemAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter.registerRenderer(PeerItemRenderer())

        startIpv8()
    }

    override fun onDestroy() {
        ipv8?.stop()
        super.onDestroy()
    }

    private fun startIpv8() {
        // General community
        val myKey = LibNaClSK.generate()
        val address = Address("0.0.0.0", 8090)
        val myPeer = Peer(myKey, address, false)
        val endpoint = UdpEndpoint(8090, InetAddress.getByName("0.0.0.0"))
        val network = Network()
        val community = DiscoveryCommunity(myPeer, endpoint, network)
        val randomWalk = RandomWalk(community, timeout = 3.0)
        val randomChurn = RandomChurn(community)
        val periodicSimilarity = PeriodicSimilarity(community)
        val overlayConfig = OverlayConfiguration(community, listOf(randomWalk, randomChurn, periodicSimilarity))

        val config = Ipv8Configuration(overlays = listOf(overlayConfig), walkerInterval = 1.0)
        ipv8 = Ipv8(endpoint, config)
        ipv8?.start()

        loadNetworkInfo(network, community.serviceId)
    }

    private fun loadNetworkInfo(network: Network, serviceId: String) {
        handler.postDelayed({
            val peers = network.getPeersForService(serviceId)
            Log.d("MainActivity", "Found ${peers.size} community peers")
            for (peer in peers) {
                Log.d("MainActivity", "$peer")
            }
            val items = peers.map { PeerItem(it) }
            adapter.updateItems(items)
            loadNetworkInfo(network, serviceId)
        }, 1000)
    }
}
