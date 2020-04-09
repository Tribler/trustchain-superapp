package nl.tudelft.trustchain.currencyii

import androidx.navigation.ui.AppBarConfiguration
import nl.tudelft.trustchain.common.BaseActivity

class MainActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_graph

    override val appBarConfiguration by lazy {
        val topLevelDestinationIds = setOf(R.id.blockchainDownloadFragment, R.id.daoLoginChoice)
        AppBarConfiguration(topLevelDestinationIds)
    }
}
