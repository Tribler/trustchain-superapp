package nl.tudelft.trustchain.detoks

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import androidx.preference.PreferenceManager
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.android.keyvault.AndroidCryptoProvider
import android.os.IBinder
import android.util.Log
import nl.tudelft.trustchain.common.BaseActivity
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
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

        val community = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!
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
