package nl.tudelft.trustchain.trader.ui

import nl.tudelft.trustchain.common.BaseActivity
import nl.tudelft.trustchain.trader.R

class TrustChainTraderActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_graph_trader
    override val bottomNavigationMenu = R.menu.bottom_navigation_menu_trader
}
