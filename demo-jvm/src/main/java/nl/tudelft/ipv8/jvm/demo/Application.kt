package nl.tudelft.ipv8.jvm.demo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import nl.tudelft.ipv8.*
import nl.tudelft.ipv8.keyvault.JavaCryptoProvider
import nl.tudelft.ipv8.messaging.Endpoint
import nl.tudelft.ipv8.messaging.udp.UdpEndpoint
import nl.tudelft.ipv8.peerdiscovery.DiscoveryCommunity
import nl.tudelft.ipv8.peerdiscovery.Network
import nl.tudelft.ipv8.peerdiscovery.strategy.PeriodicSimilarity
import nl.tudelft.ipv8.peerdiscovery.strategy.RandomChurn
import nl.tudelft.ipv8.peerdiscovery.strategy.RandomWalk
import java.net.InetAddress
import java.util.*
import kotlin.math.roundToInt

class Application {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val logger = KotlinLogging.logger {}

    fun run() {
        startIpv8()
    }

    private fun createDiscoveryCommunity(myPeer: Peer, endpoint: Endpoint, network: Network): OverlayConfiguration {
        val community = DiscoveryCommunity(myPeer, endpoint, network, maxPeers = 30, cryptoProvider = JavaCryptoProvider)
        val randomWalk = RandomWalk(community, timeout = 3.0, peers = 20)
        val randomChurn = RandomChurn(community)
        val periodicSimilarity = PeriodicSimilarity(community)
        val overlayConfig = OverlayConfiguration(community, listOf(randomWalk, randomChurn, periodicSimilarity))
        return overlayConfig
    }

    private fun createDemoCommunity(myPeer: Peer, endpoint: Endpoint, network: Network): OverlayConfiguration {
        val demoCommunity = DemoCommunity(myPeer, endpoint, network, JavaCryptoProvider)
        val demoRandomWalk = RandomWalk(demoCommunity, timeout = 3.0, peers = 20)
        val demoOverlayConfig = OverlayConfiguration(demoCommunity, listOf(demoRandomWalk))
        return demoOverlayConfig
    }

    private fun startIpv8() {
        // General community
        val myKey = JavaCryptoProvider.generateKey()
        val myPeer = Peer(myKey)
        val endpoint = UdpEndpoint(8090, InetAddress.getByName("0.0.0.0"))
        val network = Network()

        val discoveryCommunity = createDiscoveryCommunity(myPeer, endpoint, network)
        val demoCommunity = createDemoCommunity(myPeer, endpoint, network)

        val config = Ipv8Configuration(overlays = listOf(discoveryCommunity, demoCommunity), walkerInterval = 1.0)

        val ipv8 = Ipv8(endpoint, config)
        ipv8.start()

        scope.launch {
            while (true) {
                printPeersInfo(discoveryCommunity.overlay)
                printPeersInfo(demoCommunity.overlay)
                logger.info("---")
                delay(1000)
            }
        }
    }

    private fun printPeersInfo(overlay: Overlay) {
        val peers = overlay.getPeers()
        logger.info(overlay::class.simpleName + ": ${peers.size} peers")
        for (peer in peers) {
            val avgPing = peer.getAveragePing()
            val lastRequest = peer.lastRequest
            val lastResponse = peer.lastResponse

            val lastRequestStr = if (lastRequest != null)
                "" + ((Date().time - lastRequest.time) / 1000.0).roundToInt() + " s" else "?"

            val lastResponseStr = if (lastResponse != null)
                "" + ((Date().time - lastResponse.time) / 1000.0).roundToInt() + " s" else "?"

            val avgPingStr = if (!avgPing.isNaN()) "" + (avgPing * 1000).roundToInt() + " ms" else "? ms"
            logger.info("${peer.mid} (S: ${lastRequestStr}, R: ${lastResponseStr}, ${avgPingStr})")
        }
    }
}

fun main() {
    val app = Application()
    app.run()
}
