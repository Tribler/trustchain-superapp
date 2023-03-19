package nl.tudelft.trustchain.offlinemoney

import nl.tudelft.trustchain.common.BaseActivity

class MainActivityOfflineMoney() : BaseActivity() {
    override val navigationGraph = R.navigation.nav_graph_offlinemoney
    override val bottomNavigationMenu = R.menu.offlinemoney_menu
}
