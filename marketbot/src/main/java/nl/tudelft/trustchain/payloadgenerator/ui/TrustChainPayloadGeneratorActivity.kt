package nl.tudelft.trustchain.payloadgenerator.ui


import android.os.Bundle
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import nl.tudelft.trustchain.common.BaseActivity
import nl.tudelft.trustchain.common.messaging.TradePayload
import nl.tudelft.trustchain.payloadgenerator.R
import nl.tudelft.trustchain.payloadgenerator.R.id.navHostFragment


class TrustChainPayloadGeneratorActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_graph_payloadgenerator

    companion object PayloadsList{
        var payloads: MutableList<TradePayload> = mutableListOf()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appBarConfiguration = AppBarConfiguration.Builder(
            setOf(R.id.payloadFragment)
        ).build()
        this.setupActionBarWithNavController(
            findNavController(navHostFragment),
            appBarConfiguration
        )
    }

}
