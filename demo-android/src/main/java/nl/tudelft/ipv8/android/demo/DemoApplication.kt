package nl.tudelft.ipv8.android.demo

import androidx.preference.PreferenceManager
import nl.tudelft.ipv8.Ipv8Configuration
import nl.tudelft.ipv8.OverlayConfiguration
import nl.tudelft.ipv8.android.keyvault.AndroidCryptoProvider
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.peerdiscovery.strategy.RandomWalk
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex

class DemoApplication : Ipv8Application() {
    override fun getPrivateKey(): PrivateKey {
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

    override fun getIpv8Configuration(): Ipv8Configuration {
        val discoveryCommunity = createDiscoveryCommunity()
        val trustChainCommunity = createTrustChainCommunity()
        val demoCommunity = createDemoCommunity(trustChainCommunity.overlay)
        return Ipv8Configuration(overlays = listOf(
            discoveryCommunity,
            trustChainCommunity,
            demoCommunity
        ), walkerInterval = 1.0)
    }

    private fun createDemoCommunity(
        trustChainCommunity: TrustChainCommunity
    ): OverlayConfiguration<DemoCommunity> {
        val demoCommunity = DemoCommunity(
            myPeer, endpoint, network, AndroidCryptoProvider, trustChainCommunity
        )
        val demoRandomWalk = RandomWalk(demoCommunity, timeout = 3.0, peers = 20)
        return OverlayConfiguration(demoCommunity, listOf(demoRandomWalk))
    }

    companion object {
        private const val PREF_PRIVATE_KEY = "private_key"
    }
}
