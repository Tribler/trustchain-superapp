package nl.tudelft.ipv8.android.demo.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import nl.tudelft.ipv8.*
import nl.tudelft.ipv8.android.demo.DemoCommunity
import nl.tudelft.ipv8.android.demo.Ipv8Application
import nl.tudelft.ipv8.android.demo.R
import nl.tudelft.ipv8.android.demo.ui.peers.MainActivity
import nl.tudelft.ipv8.android.keyvault.AndroidCryptoProvider
import nl.tudelft.ipv8.android.messaging.udp.AndroidUdpEndpoint
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainSettings
import nl.tudelft.ipv8.attestation.trustchain.store.Database
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainSQLiteStore
import nl.tudelft.ipv8.messaging.Endpoint
import nl.tudelft.ipv8.peerdiscovery.DiscoveryCommunity
import nl.tudelft.ipv8.peerdiscovery.Network
import nl.tudelft.ipv8.peerdiscovery.strategy.PeriodicSimilarity
import nl.tudelft.ipv8.peerdiscovery.strategy.RandomChurn
import nl.tudelft.ipv8.peerdiscovery.strategy.RandomWalk
import java.net.InetAddress
import java.util.*
import kotlin.math.roundToInt

class Ipv8Service : Service() {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val logger = KotlinLogging.logger {}

    private var ipv8: Ipv8? = null
    private var isBound = false

    override fun onCreate() {
        super.onCreate()

        startIpv8()
        showForegroundNotification()
    }

    override fun onDestroy() {
        ipv8?.stop()

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        isBound = true
        showForegroundNotification()
        return LocalBinder()
    }

    override fun onRebind(intent: Intent?) {
        isBound = true
        showForegroundNotification()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isBound = false
        showForegroundNotification()
        return true
    }

    fun getOverlays(): List<Overlay> {
        val ipv8 = ipv8 ?: throw IllegalStateException("IPv8 is not running")
        return ipv8.getOverlays()
    }

    private fun createDiscoveryCommunity(
        myPeer: Peer,
        endpoint: Endpoint,
        network: Network
    ): OverlayConfiguration<DiscoveryCommunity> {
        val community = DiscoveryCommunity(myPeer, endpoint, network, maxPeers = 30,
            cryptoProvider = AndroidCryptoProvider
        )
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
        val driver: SqlDriver = AndroidSqliteDriver(Database.Schema, this, "trustchain.db")
        val database = Database(driver)
        val store = TrustChainSQLiteStore(database)
        val trustChainCommunity = TrustChainCommunity(myPeer, endpoint, network, maxPeers = 30,
            cryptoProvider = AndroidCryptoProvider, settings = settings, database = store)
        val randomWalk = RandomWalk(trustChainCommunity, timeout = 3.0, peers = 20)
        return OverlayConfiguration(trustChainCommunity, listOf(randomWalk))
    }

    private fun createDemoCommunity(
        myPeer: Peer,
        endpoint: Endpoint,
        network: Network,
        trustChainCommunity: TrustChainCommunity
    ): OverlayConfiguration<DemoCommunity> {
        val demoCommunity = DemoCommunity(
            myPeer, endpoint, network, AndroidCryptoProvider,
            trustChainCommunity
        )
        val demoRandomWalk = RandomWalk(demoCommunity, timeout = 3.0, peers = 20)
        return OverlayConfiguration(demoCommunity, listOf(demoRandomWalk))
    }

    private fun startIpv8() {
        val myKey = AndroidCryptoProvider.generateKey()
        val myPeer = Peer(myKey)
        val connectivityManager = getSystemService<ConnectivityManager>()!!
        val endpoint = AndroidUdpEndpoint(8090, InetAddress.getByName("0.0.0.0"),
            connectivityManager)
        val network = Network()

        val discoveryCommunity = createDiscoveryCommunity(myPeer, endpoint, network)
        val trustChainCommunity = createTrustChainCommunity(myPeer, endpoint, network)
        val demoCommunity = createDemoCommunity(myPeer, endpoint, network,
            trustChainCommunity.overlay)

        val config = Ipv8Configuration(overlays = listOf(
            discoveryCommunity,
            trustChainCommunity,
            demoCommunity
        ), walkerInterval = 1.0)

        val ipv8 = Ipv8(endpoint, config)
        this.ipv8 = ipv8
        ipv8.start()

        scope.launch {
            while (true) {
                for (overlay in getOverlays()) {
                    printPeersInfo(overlay)
                }
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

    private fun showForegroundNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val cancelBroadcastIntent = Intent(this, CancelIpv8Receiver::class.java)
        val cancelPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            0, cancelBroadcastIntent, 0
        )

        val builder = NotificationCompat.Builder(this,
            Ipv8Application.NOTIFICATION_CHANNEL_CONNECTION)
            .setContentTitle("IPv8")
            .setContentText("Running")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
        // Allow cancellation when the app is running in background
        if (!isBound) {
            builder.addAction(NotificationCompat.Action(0, "Stop", cancelPendingIntent))
        }
        startForeground(
            ONGOING_NOTIFICATION_ID,
            builder.build()
        )
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        val service: Ipv8Service
            get() = this@Ipv8Service
    }

    companion object {
        private const val ONGOING_NOTIFICATION_ID = 1
    }
}
