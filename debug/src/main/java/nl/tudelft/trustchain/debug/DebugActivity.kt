package nl.tudelft.trustchain.debug

import nl.tudelft.trustchain.common.DrawerActivity

class DebugActivity : DrawerActivity() {
    override val navigationGraph = R.navigation.nav_graph_debug
    override val topLevelDestinationIds = setOf(R.id.debugFragment)
    override val drawerNavigationItem = R.id.nav_debug
}
