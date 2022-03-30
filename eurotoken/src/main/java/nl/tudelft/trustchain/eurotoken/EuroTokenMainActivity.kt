package nl.tudelft.trustchain.eurotoken

import nl.tudelft.trustchain.common.BaseActivity

const val EUROTOKEN_PREFERENCES = "eurotoken"
const val DEMO_MODE_ENABLED_PREF = "demo_mode_enabled"

class EuroTokenMainActivity : BaseActivity() {

    override val navigationGraph = R.navigation.nav_graph_eurotoken
    override val bottomNavigationMenu = R.menu.eurotoken_navigation_menu


}
