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

    }

}
