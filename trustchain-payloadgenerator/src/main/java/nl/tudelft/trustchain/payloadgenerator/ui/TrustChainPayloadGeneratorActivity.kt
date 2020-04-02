package nl.tudelft.trustchain.payloadgenerator.ui

import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.squareup.sqldelight.android.AndroidSqliteDriver
import kotlinx.coroutines.delay
import nl.tudelft.ipv8.*
import nl.tudelft.ipv8.android.keyvault.AndroidCryptoProvider
import nl.tudelft.ipv8.android.messaging.bluetooth.BluetoothLeEndpoint
import nl.tudelft.ipv8.android.messaging.bluetooth.GattServerManager
import nl.tudelft.ipv8.android.messaging.bluetooth.IPv8BluetoothLeAdvertiser
import nl.tudelft.ipv8.android.messaging.udp.AndroidUdpEndpoint
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.messaging.EndpointAggregator
import nl.tudelft.ipv8.peerdiscovery.Network
import nl.tudelft.ipv8.peerdiscovery.strategy.RandomWalk
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.BaseActivity
import nl.tudelft.trustchain.common.messaging.TradePayload
import nl.tudelft.trustchain.common.MarketCommunity
import nl.tudelft.trustchain.payloadgenerator.R
import nl.tudelft.trustchain.payloadgenerator.R.id.navHostFragment
import java.net.InetAddress
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.BlockListener
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainSettings
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainSQLiteStore
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.sqldelight.Database
import nl.tudelft.trustchain.common.constants.Currency
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.TrustChainHelper
import kotlin.concurrent.thread
import nl.tudelft.trustchain.payloadgenerator.constants.BlockTypes


import java.util.*


class TrustChainPayloadGeneratorActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_graph_payloadgenerator

    companion object PayloadsList{
        var payloads: MutableList<TradePayload> = mutableListOf()
    }
    protected fun getTrustChainCommunity(ipv8: IPv8): TrustChainCommunity {
        return ipv8.getOverlay() ?: throw IllegalStateException("TrustChainCommunity is not configured")
    }

    protected fun getMarketCommunity(ipv8: IPv8): MarketCommunity {
        return ipv8.getOverlay() ?: throw java.lang.IllegalStateException("MarketCommunity is not configured")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appBarConfiguration = AppBarConfiguration.Builder(
            setOf(
            )
        ).build()
        this.setupActionBarWithNavController(
            findNavController(navHostFragment),
            appBarConfiguration
        )

        val ipv8 = startMarketBot()
        setListener(ipv8)
        sendAutoMessages(ipv8)
    }

    fun setListener(ipv8: IPv8){
        lifecycleScope.launchWhenStarted {
            val trustchain = getTrustChainCommunity(ipv8)
            Log.d("Setlistener","Listener runs")
//            trustchain.registerTransactionValidator(BlockTypes.DEMO_TX_BLOCK.value, DDValidator())

            trustchain.addListener(BlockTypes.DEMO_TX_BLOCK.value, object : BlockListener {
                override fun onBlockReceived(block: TrustChainBlock) {
                    Log.d(
                        "TrustChainDemo",
                        "onBlockReceived: ${block.blockId} ${block.transaction}"
                    )
//                    trustchain.createAgreementBlock(block, block.transaction)
                }
            })
        }
    }

    fun startMarketBot():IPv8 {
        val ipv8 = create()
        if (!ipv8.isStarted()){
            ipv8.start()
        }

        println("Serivce IDD: " + ipv8.getOverlay<MarketCommunity>())
        return ipv8
    }

    fun sendAutoMessages(ipv8: IPv8){
        thread(start = true) {
            while (true) {
                Thread.sleep(1000)
                val marketCommunity = getMarketCommunity(ipv8)
                val r = Random()
                val typeInt = kotlin.random.Random.nextInt(0,2)
                var type = "Bid"
                var availableAmount = r.nextGaussian()*15+100
                var requiredAmount = 1.0
                if (typeInt==1){
                    type = "Ask"
                    availableAmount = 1.0
                    requiredAmount = r.nextGaussian()*15+100
                }
                availableAmount = String.format("%.2f", availableAmount).toDouble()
                requiredAmount = String.format("%.2f", requiredAmount).toDouble()
                val payloadSerializable =
                    createPayloadSerializable(availableAmount, requiredAmount, type, ipv8)
                marketCommunity.broadcast(payloadSerializable)
                Log.d("TrustChainPayloadGeneratorActivity::sendAutoMessages", "message send!")
            }
        }
    }

    fun create(): IPv8 {
        val privateKey = getPrivateKey()
        val configuration = IPv8Configuration(
            overlays = listOf(
                createMarketCommunity(),
                createTrustChainCommunity()
            ), walkerInterval = 5.0
        )
        val connectivityManager = application.getSystemService<ConnectivityManager>()
            ?: throw IllegalStateException("ConnectivityManager not found")

        val udpEndpoint = AndroidUdpEndpoint(8091, InetAddress.getByName("0.0.0.0"),
            connectivityManager)

        val bluetoothManager = application.getSystemService<BluetoothManager>()
            ?: throw IllegalStateException("BluetoothManager not found")

        val myPeer = Peer(privateKey)
        val network = Network()

        val gattServer = GattServerManager(application, myPeer)
        val bleAdvertiser = IPv8BluetoothLeAdvertiser(bluetoothManager)
        val bluetoothEndpoint = if (bluetoothManager.adapter != null)
            BluetoothLeEndpoint(application, bluetoothManager, gattServer, bleAdvertiser,
                network) else null

        val endpointAggregator = EndpointAggregator(
            udpEndpoint,
            bluetoothEndpoint
        )
        return IPv8(endpointAggregator, configuration, myPeer, AndroidCryptoProvider, network)
    }

    private fun getPrivateKey(): PrivateKey {
        // Load a key from the shared preferences
        val key = "Market_bot_key"
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val privateKey = prefs.getString(key, null)
        return if (privateKey == null) {
            // Generate a new key on the first launch
            val newKey = AndroidCryptoProvider.generateKey()
            prefs.edit()
                .putString(key, newKey.keyToBin().toHex())
                .apply()
            newKey
        } else {
            AndroidCryptoProvider.keyFromPrivateBin(privateKey.hexToBytes())
        }
    }

    private fun createMarketCommunity(): OverlayConfiguration<MarketCommunity> {
        val randomWalk = RandomWalk.Factory()
        return OverlayConfiguration(
            Overlay.Factory(MarketCommunity::class.java),
            listOf(randomWalk)
        )
    }

    private fun createTrustChainCommunity(): OverlayConfiguration<TrustChainCommunity> {
        val settings = TrustChainSettings()
        val driver = AndroidSqliteDriver(Database.Schema, this, "trustchain2.db")
        val store = TrustChainSQLiteStore(Database(driver))
        val randomWalk = RandomWalk.Factory()
        return OverlayConfiguration(
            TrustChainCommunity.Factory(settings, store),
            listOf(randomWalk)
        )
    }

    fun createPayloadSerializable(
        availableAmount: Double,
        requiredAmount: Double,
        type: String?,
        ipv8:IPv8
    ): Serializable {
        val trustchain = TrustChainHelper(getTrustChainCommunity(ipv8))
        var primaryCurrency = Currency.DYMBE_DOLLAR
        var secondaryCurrency = Currency.BTC
        var type2 = TradePayload.Type.BID
        if (type.equals("Ask")) {
            primaryCurrency = Currency.BTC
            secondaryCurrency = Currency.DYMBE_DOLLAR
            type2 = TradePayload.Type.ASK
        }
        return TradePayload(
            trustchain.getMyPublicKey(),
            primaryCurrency,
            secondaryCurrency,
            availableAmount,
            requiredAmount,
            type2
        )
    }

}
