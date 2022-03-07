package nl.tudelft.trustchain.atomicswap

import android.app.Application
import android.bluetooth.BluetoothManager
import android.os.Build
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import nl.tudelft.ipv8.IPv8Configuration
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.OverlayConfiguration
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.android.keyvault.AndroidCryptoProvider
import nl.tudelft.ipv8.android.messaging.bluetooth.BluetoothLeDiscovery
import nl.tudelft.ipv8.android.peerdiscovery.NetworkServiceDiscovery
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.peerdiscovery.DiscoveryCommunity
import nl.tudelft.ipv8.peerdiscovery.strategy.PeriodicSimilarity
import nl.tudelft.ipv8.peerdiscovery.strategy.RandomChurn
import nl.tudelft.ipv8.peerdiscovery.strategy.RandomWalk
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex

class AtomicSwapApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        defaultCryptoProvider = AndroidCryptoProvider


        val BUILD_VERSION_CODE_S = 31
        if (Build.VERSION.SDK_INT < BUILD_VERSION_CODE_S) {
            initIPv8()
        }
    }

    fun initIPv8() {
        val config = IPv8Configuration(overlays = listOf(
            createDiscoveryCommunity(),
            createAtomicSwapCommunity()
        ), walkerInterval = 5.0)

        IPv8Android.Factory(this)
            .setConfiguration(config)
            .setPrivateKey(getPrivateKey())
            .init()

    }

    private fun createDiscoveryCommunity(): OverlayConfiguration<DiscoveryCommunity> {
        val randomWalk = RandomWalk.Factory()
        val randomChurn = RandomChurn.Factory()
        val periodicSimilarity = PeriodicSimilarity.Factory()

        val nsd = NetworkServiceDiscovery.Factory(getSystemService()!!)
        val bluetoothManager = getSystemService<BluetoothManager>()
            ?: throw IllegalStateException("BluetoothManager not available")
        val strategies = mutableListOf(
            randomWalk, randomChurn, periodicSimilarity, nsd
        )
        if (bluetoothManager.adapter != null && Build.VERSION.SDK_INT >= 24) {
            val ble = BluetoothLeDiscovery.Factory()
            strategies += ble
        }

        return OverlayConfiguration(
            DiscoveryCommunity.Factory(),
            strategies
        )
    }

    private fun createAtomicSwapCommunity(): OverlayConfiguration<AtomicSwapCommunity> {
        val randomWalk = RandomWalk.Factory()
        return OverlayConfiguration(
            Overlay.Factory(AtomicSwapCommunity::class.java),
            listOf(randomWalk)
        )
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
