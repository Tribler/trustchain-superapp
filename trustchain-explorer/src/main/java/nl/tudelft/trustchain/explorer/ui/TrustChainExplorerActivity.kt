package nl.tudelft.trustchain.explorer.ui

import nl.tudelft.trustchain.common.BaseActivity
import nl.tudelft.trustchain.explorer.R

class TrustChainExplorerActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_graph_explorer
    override val bottomNavigationMenu = R.menu.bottom_navigation_menu
}
