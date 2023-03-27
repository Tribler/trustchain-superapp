package nl.tudelft.trustchain.detoks

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import com.squareup.sqldelight.android.AndroidSqliteDriver
import nl.tudelft.ipv8.IPv8Configuration
import nl.tudelft.ipv8.OverlayConfiguration
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.android.keyvault.AndroidCryptoProvider
import nl.tudelft.ipv8.android.peerdiscovery.NetworkServiceDiscovery
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainSQLiteStore
import android.os.IBinder
import android.util.Log
import nl.tudelft.trustchain.common.BaseActivity
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.peerdiscovery.strategy.RandomWalk
import nl.tudelft.ipv8.sqldelight.Database
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex

class DeToksActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_graph_detoks
    var gossipService: GossiperService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actionBar = supportActionBar
        defaultCryptoProvider = AndroidCryptoProvider
        actionBar!!.hide()

        Intent(this, GossiperService::class.java).also { intent ->
            startService(intent)
            bindService(intent, gossipConnection, Context.BIND_AUTO_CREATE)
        }
        // TODO: we can put this in app/trustChainApplication.kt like the other apps
        createLikeCommunity()

        val community = IPv8Android.getInstance().getOverlay<LikeCommunity>()!!
        val peers = community.getPeers()
        for (peer in peers) {
            Log.d("DeToks", peer.mid)
        }
    }

    private val gossipConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as GossiperService.LocalBinder
            gossipService = binder.getService()
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            gossipService = null
        }
    }

    fun createLikeCommunity() {
        val settings = TrustChainSettings()
        val driver = AndroidSqliteDriver(Database.Schema, this, "trustchain.db")
        val store = TrustChainSQLiteStore(Database(driver))
        val deToksComm = OverlayConfiguration(
            LikeCommunity.Factory(settings, store),
            listOf(
                RandomWalk.Factory(),
                NetworkServiceDiscovery.Factory(getSystemService()!!)
            )
        )

        val config = IPv8Configuration(overlays = listOf(
            deToksComm
        ))

        IPv8Android.Factory(this.application!!)
            .setConfiguration(config)
            .setPrivateKey(getPrivateKey())
            .init()
    }

    private fun getPrivateKey(): PrivateKey {
        // Load a key from the shared preferences
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val privateKey = prefs.getString(PREF_PRIVATE_KEY, null)
        return if (privateKey == null) {
            // Generate a new key on the first launch
            val newKey = AndroidCryptoProvider.generateKey()
            prefs.edit()
                .putString(PREF_PRIVATE_KEY, newKey.keyToBin().toHex())
                .apply()
            newKey
        } else {
            AndroidCryptoProvider.keyFromPrivateBin(privateKey.hexToBytes())
        }
    }

    companion object {
        private const val PREF_PRIVATE_KEY = "private_key"
        private const val BLOCK_TYPE = "demo_block"
    }
}
