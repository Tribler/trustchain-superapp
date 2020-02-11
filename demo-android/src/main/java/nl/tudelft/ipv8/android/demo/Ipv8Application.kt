package nl.tudelft.ipv8.android.demo

import android.app.Application
import android.content.Intent
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import nl.tudelft.ipv8.Ipv8
import nl.tudelft.ipv8.Ipv8Configuration
import nl.tudelft.ipv8.OverlayConfiguration
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.demo.service.Ipv8Service
import nl.tudelft.ipv8.android.keyvault.AndroidCryptoProvider
import nl.tudelft.ipv8.android.messaging.udp.AndroidUdpEndpoint
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainSettings
import nl.tudelft.ipv8.attestation.trustchain.store.Database
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainSQLiteStore
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.peerdiscovery.DiscoveryCommunity
import nl.tudelft.ipv8.peerdiscovery.Network
import nl.tudelft.ipv8.peerdiscovery.strategy.PeriodicSimilarity
import nl.tudelft.ipv8.peerdiscovery.strategy.RandomChurn
import nl.tudelft.ipv8.peerdiscovery.strategy.RandomWalk
import java.net.InetAddress

abstract class Ipv8Application : Application() {
    private val _ipv8 by lazy {
        Ipv8(endpoint, getIpv8Configuration())
    }

    val ipv8: Ipv8
        get() {
            if (!_ipv8.isStarted()) {
                _ipv8.start()
                startService()
            }
            return _ipv8
        }

    val myPeer by lazy {
        Peer(getPrivateKey())
    }

    val endpoint by lazy {
        val connectivityManager = getSystemService<ConnectivityManager>()!!
        AndroidUdpEndpoint(8090, InetAddress.getByName("0.0.0.0"), connectivityManager)
    }

    val network by lazy {
        Network()
    }

    private fun startService() {
        val serviceIntent = Intent(this, Ipv8Service::class.java)
        startForegroundService(serviceIntent)
    }

    abstract fun getIpv8Configuration(): Ipv8Configuration

    abstract fun getPrivateKey(): PrivateKey

    protected fun createDiscoveryCommunity(): OverlayConfiguration<DiscoveryCommunity> {
        val community = DiscoveryCommunity(myPeer, endpoint, network, maxPeers = 30,
            cryptoProvider = AndroidCryptoProvider
        )
        val randomWalk = RandomWalk(community, timeout = 3.0, peers = 20)
        val randomChurn = RandomChurn(community)
        val periodicSimilarity = PeriodicSimilarity(community)
        return OverlayConfiguration(community, listOf(randomWalk, randomChurn, periodicSimilarity))
    }

    protected fun createTrustChainCommunity(): OverlayConfiguration<TrustChainCommunity> {
        val settings = TrustChainSettings()
        val driver: SqlDriver = AndroidSqliteDriver(Database.Schema, this, "trustchain.db")
        val database = Database(driver)
        val store = TrustChainSQLiteStore(database)
        val trustChainCommunity = TrustChainCommunity(myPeer, endpoint, network, maxPeers = 30,
            cryptoProvider = AndroidCryptoProvider, settings = settings, database = store)
        val randomWalk = RandomWalk(trustChainCommunity, timeout = 3.0, peers = 20)
        return OverlayConfiguration(trustChainCommunity, listOf(randomWalk))
    }
}
