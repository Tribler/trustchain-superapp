package nl.tudelft.trustchain.offlinemoney

import android.os.Build
import android.os.Bundle
import nl.tudelft.trustchain.app.TrustChainApplication
import nl.tudelft.trustchain.common.BaseActivity

class MainActivityOfflineMoney() : BaseActivity() {
    override val navigationGraph = R.navigation.nav_graph_offlinemoney

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TrustChainApplication().initIPv8()
    }
}
