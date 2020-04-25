package nl.tudelft.trustchain.currencyii

import androidx.navigation.ui.AppBarConfiguration
import nl.tudelft.trustchain.common.BaseActivity

class CurrencyIIMainActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_graph
    override val bottomNavigationMenu = R.menu.currencyii_bottom_navigation_menu

    override val appBarConfiguration by lazy {
        val topLevelDestinationIds = setOf(R.id.blockchainDownloadFragment, R.id.daoLoginChoice)
        AppBarConfiguration(topLevelDestinationIds)
    }
}
