package nl.tudelft.ipv8.android.demo

import android.app.Application
import androidx.preference.PreferenceManager
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import nl.tudelft.ipv8.*
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.android.IPv8AndroidFactory
import nl.tudelft.ipv8.android.demo.service.DemoService
import nl.tudelft.ipv8.android.keyvault.AndroidCryptoProvider
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainSettings
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainSQLiteStore
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.peerdiscovery.DiscoveryCommunity
import nl.tudelft.ipv8.peerdiscovery.strategy.PeriodicSimilarity
import nl.tudelft.ipv8.peerdiscovery.strategy.RandomChurn
import nl.tudelft.ipv8.peerdiscovery.strategy.RandomWalk
import nl.tudelft.ipv8.sqldelight.Database
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex

class DemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initIPv8()
    }

    private fun initIPv8() {
        val config = IPv8Configuration(overlays = listOf(
            createDiscoveryCommunity(),
            createTrustChainCommunity(),
            createDemoCommunity()
        ), walkerInterval = 1.0)

        val factory = IPv8AndroidFactory(this)
            .setConfiguration(config)
            .setPrivateKey(getPrivateKey())

        IPv8Android.init(this, factory, DemoService::class.java)
    }

    private fun createDiscoveryCommunity(): OverlayConfiguration<DiscoveryCommunity> {
        val randomWalk = RandomWalk.Factory(timeout = 3.0, peers = 20)
        val randomChurn = RandomChurn.Factory()
        val periodicSimilarity = PeriodicSimilarity.Factory()
        return OverlayConfiguration(
            DiscoveryCommunity.Factory(),
            listOf(randomWalk, randomChurn, periodicSimilarity)
        )
    }

    private fun createTrustChainCommunity(): OverlayConfiguration<TrustChainCommunity> {
        val settings = TrustChainSettings()
        val driver: SqlDriver = AndroidSqliteDriver(Database.Schema, this, "trustchain.db")
        val database = Database(driver)
        val store = TrustChainSQLiteStore(database)
        val randomWalk = RandomWalk.Factory(timeout = 3.0, peers = 20)
        return OverlayConfiguration(
            TrustChainCommunity.Factory(settings, store),
            listOf(randomWalk)
        )
    }

    private fun createDemoCommunity(): OverlayConfiguration<DemoCommunity> {
        val randomWalk = RandomWalk.Factory(timeout = 3.0, peers = 20)
        return OverlayConfiguration(
            Overlay.Factory(DemoCommunity::class.java),
            listOf(randomWalk)
        )
    }

    private fun getPrivateKey(): PrivateKey {
        // Return key from the shared preferences
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val privateKey = prefs.getString(PREF_PRIVATE_KEY, null)
        if (privateKey == null) {
            // Generate a new key on the first launch
            val newKey = AndroidCryptoProvider.generateKey()
            prefs.edit()
                .putString(PREF_PRIVATE_KEY, newKey.keyToBin().toHex())
                .apply()
            return newKey
        }
        return AndroidCryptoProvider.keyFromPrivateBin(privateKey.hexToBytes())
    }

    companion object {
        private const val PREF_PRIVATE_KEY = "private_key"
    }
}
