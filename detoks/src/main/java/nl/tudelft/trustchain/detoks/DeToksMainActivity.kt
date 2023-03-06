package nl.tudelft.trustchain.detoks

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.common.BaseActivity

class DeToksActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_graph_detoks

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actionBar = supportActionBar
        actionBar!!.hide()
//        broadcast()
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
