package nl.tudelft.trustchain.detoks_engine

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.common.BaseActivity

class DeToksEngineMainActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_graph_detoks_engine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actionBar = supportActionBar
        actionBar!!.hide()
    }

    private fun broadcast() {
        val community = IPv8Android.getInstance().getOverlay<TransactionCommunity>()!!
        lifecycleScope.launch {
            while (isActive) {
                community.broadcastGreeting()
                delay(1000)
            }
        }
    }
}
