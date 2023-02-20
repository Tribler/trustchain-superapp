package nl.tudelft.trustchain.detoks

import android.os.Bundle
import nl.tudelft.trustchain.common.BaseActivity

class DeToksActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_graph_detoks

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actionBar = supportActionBar
        actionBar!!.hide()
    }
}
