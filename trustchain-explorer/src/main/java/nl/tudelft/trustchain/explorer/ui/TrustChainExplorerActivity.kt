package nl.tudelft.trustchain.explorer.ui

import nl.tudelft.trustchain.common.DrawerActivity
import nl.tudelft.trustchain.explorer.R

class TrustChainExplorerActivity : DrawerActivity() {
    override val navigationGraph = R.navigation.nav_graph_explorer
    override val topLevelDestinationIds = setOf(R.id.peersFragment, R.id.usersFragment,
        R.id.latestBlocksFragment, R.id.myChainFragment, R.id.debugFragment)
    override val drawerNavigationItem = R.id.nav_trustchain_explorer
    override val bottomNavigationMenu = R.menu.bottom_navigation_menu
}
