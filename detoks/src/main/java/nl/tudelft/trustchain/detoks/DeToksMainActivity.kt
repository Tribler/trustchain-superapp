package nl.tudelft.trustchain.detoks

import android.bluetooth.BluetoothManager
import android.net.nsd.NsdManager
import android.os.Bundle
import android.security.KeyChain.getPrivateKey
import android.util.Log
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import com.squareup.sqldelight.android.AndroidSqliteDriver
import nl.tudelft.ipv8.IPv8Configuration
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.OverlayConfiguration
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.android.keyvault.AndroidCryptoProvider
import nl.tudelft.ipv8.android.peerdiscovery.NetworkServiceDiscovery
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainSQLiteStore
import nl.tudelft.trustchain.common.BaseActivity
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.attestation.trustchain.validation.TransactionValidator
import nl.tudelft.ipv8.attestation.trustchain.validation.ValidationResult
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.peerdiscovery.DiscoveryCommunity
import nl.tudelft.ipv8.peerdiscovery.strategy.DiscoveryStrategy
import nl.tudelft.ipv8.peerdiscovery.strategy.PeriodicSimilarity
import nl.tudelft.ipv8.peerdiscovery.strategy.RandomChurn
import nl.tudelft.ipv8.peerdiscovery.strategy.RandomWalk
import nl.tudelft.ipv8.sqldelight.Database
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex

class DeToksActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_graph_detoks

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val actionBar = supportActionBar
        defaultCryptoProvider = AndroidCryptoProvider
        actionBar!!.hide()
        // Maybe we can put this in app/trustChainApplication.kt like the other apps
        createDetoksCommunity()

        val community = IPv8Android.getInstance().getOverlay<DetoksCommunity>()!!
        val peers = community.getPeers()
        for (peer in peers) {
            Log.d("DeToks", peer.mid)
        }
    }

    fun createDetoksCommunity() {
        val settings = TrustChainSettings()
        val driver = AndroidSqliteDriver(Database.Schema, this, "trustchain.db")
        val store = TrustChainSQLiteStore(Database(driver))
        val deToksComm = OverlayConfiguration(
            DetoksCommunity.Factory(settings, store),
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

//    private fun initIPv8() {
//        val config = IPv8Configuration(overlays = listOf(
//            createDiscoveryCommunity(),
//            createTrustChainCommunity(),
//            createDemoCommunity()
//        ), walkerInterval = 5.0)
//
//        IPv8Android.Factory(this.application!!)
//            .setConfiguration(config)
//            .setPrivateKey(getPrivateKey())
//            .setServiceClass(TrustChainService::class.java)
//            .init()
//
//        initTrustChain()
//    }
//
//    private fun initTrustChain() {
//        val ipv8 = IPv8Android.getInstance()
//        val trustchain = ipv8.getOverlay<TrustChainCommunity>()!!
//
//        trustchain.registerTransactionValidator(BLOCK_TYPE, object : TransactionValidator {
//            override fun validate(
//                block: TrustChainBlock,
//                database: TrustChainStore
//            ): ValidationResult {
//                if (block.transaction["message"] != null || block.isAgreement) {
//                    return ValidationResult.Valid
//                } else {
//                    return ValidationResult.Invalid(listOf(""))
//                }
//            }
//        })
//
//        trustchain.registerBlockSigner(BLOCK_TYPE, object : BlockSigner {
//            override fun onSignatureRequest(block: TrustChainBlock) {
//                trustchain.createAgreementBlock(block, mapOf<Any?, Any?>())
//            }
//        })
//
//        trustchain.addListener(BLOCK_TYPE, object : BlockListener {
//            override fun onBlockReceived(block: TrustChainBlock) {
//                Log.d("DeToks", "onBlockReceived: ${block.blockId} ${block.transaction}")
//            }
//        })
//    }
//
//    private fun createDiscoveryCommunity(): OverlayConfiguration<DiscoveryCommunity> {
//        val randomWalk = RandomWalk.Factory()
//        val randomChurn = RandomChurn.Factory()
//        val periodicSimilarity = PeriodicSimilarity.Factory()
//
//        val nsd = getSystemService<NsdManager>()?.let { NetworkServiceDiscovery.Factory(it) }
//
//        val strategies = listOf(
//            randomWalk, randomChurn, periodicSimilarity, nsd
//        )
//
//        return OverlayConfiguration(
//            DiscoveryCommunity.Factory(),
//
//            strategies.map { it as DiscoveryStrategy.Factory<*> }
//        )
//    }
//
//    private fun createTrustChainCommunity(): OverlayConfiguration<TrustChainCommunity> {
//        val settings = TrustChainSettings()
//        val driver = AndroidSqliteDriver(Database.Schema, this, "trustchain.db")
//        val store = TrustChainSQLiteStore(Database(driver))
//        val randomWalk = RandomWalk.Factory()
//        return OverlayConfiguration(
//            TrustChainCommunity.Factory(settings, store),
//            listOf(randomWalk)
//        )
//    }
//
//    private fun createDemoCommunity(): OverlayConfiguration<DetoksCommunity> {
//        val randomWalk = RandomWalk.Factory()
//        return OverlayConfiguration(
//            Overlay.Factory(DetoksCommunity::class.java),
//            listOf(randomWalk)
//        )
//    }
//
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
