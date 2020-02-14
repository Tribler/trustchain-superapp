package nl.tudelft.ipv8.jvm.demo

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver.Companion.IN_MEMORY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import nl.tudelft.ipv8.*
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainSettings
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainSQLiteStore
import nl.tudelft.ipv8.keyvault.JavaCryptoProvider
import nl.tudelft.ipv8.messaging.Endpoint
import nl.tudelft.ipv8.messaging.udp.UdpEndpoint
import nl.tudelft.ipv8.peerdiscovery.DiscoveryCommunity
import nl.tudelft.ipv8.peerdiscovery.Network
import nl.tudelft.ipv8.peerdiscovery.strategy.PeriodicSimilarity
import nl.tudelft.ipv8.peerdiscovery.strategy.RandomChurn
import nl.tudelft.ipv8.peerdiscovery.strategy.RandomWalk
import nl.tudelft.ipv8.sqldelight.Database
import java.net.InetAddress
import java.util.*
import kotlin.math.roundToInt

class Application {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val logger = KotlinLogging.logger {}

    fun run() {
        startIpv8()
    }

    private fun createDiscoveryCommunity(
        myPeer: Peer,
        endpoint: Endpoint,
        network: Network
    ): OverlayConfiguration<DiscoveryCommunity> {
        val community = DiscoveryCommunity(myPeer, endpoint, network, maxPeers = 30,
            cryptoProvider = JavaCryptoProvider)
        val randomWalk = RandomWalk(community, timeout = 3.0, peers = 20)
        val randomChurn = RandomChurn(community)
        val periodicSimilarity = PeriodicSimilarity(community)
        return OverlayConfiguration(community, listOf(randomWalk, randomChurn, periodicSimilarity))
    }

    private fun createTrustChainCommunity(
        myPeer: Peer,
        endpoint: Endpoint,
        network: Network
    ): OverlayConfiguration<TrustChainCommunity> {
        val settings = TrustChainSettings()
        val driver: SqlDriver = JdbcSqliteDriver(IN_MEMORY)
        val database = Database(driver)
        val store = TrustChainSQLiteStore(database)
        val trustChainCommunity = TrustChainCommunity(myPeer, endpoint, network, maxPeers = 30,
            cryptoProvider = JavaCryptoProvider, settings = settings, database = store)
        val randomWalk = RandomWalk(trustChainCommunity, timeout = 3.0, peers = 20)
        return OverlayConfiguration(trustChainCommunity, listOf(randomWalk))
    }

    private fun createDemoCommunity(
        myPeer: Peer,
        endpoint: Endpoint,
        network: Network,
        trustChainCommunity: TrustChainCommunity
    ): OverlayConfiguration<DemoCommunity> {
        val demoCommunity = DemoCommunity(myPeer, endpoint, network, JavaCryptoProvider,
            trustChainCommunity)
        val demoRandomWalk = RandomWalk(demoCommunity, timeout = 3.0, peers = 20)
        return OverlayConfiguration(demoCommunity, listOf(demoRandomWalk))
    }

    private fun startIpv8() {
        val myKey = JavaCryptoProvider.generateKey()
        val myPeer = Peer(myKey)
        val endpoint = UdpEndpoint(8090, InetAddress.getByName("0.0.0.0"))
        val network = Network()

        val discoveryCommunity = createDiscoveryCommunity(myPeer, endpoint, network)
        val trustChainCommunity = createTrustChainCommunity(myPeer, endpoint, network)
        val demoCommunity = createDemoCommunity(myPeer, endpoint, network, trustChainCommunity.overlay)

        val config = Ipv8Configuration(overlays = listOf(
            //discoveryCommunity,
            //trustChainCommunity,
            demoCommunity
        ), walkerInterval = 1.0)

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
