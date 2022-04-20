package nl.tudelft.trustchain.eurotoken

import nl.tudelft.trustchain.common.BaseActivity

class EuroTokenMainActivity : BaseActivity() {

    override val navigationGraph = R.navigation.nav_graph_eurotoken
    override val bottomNavigationMenu = R.menu.eurotoken_navigation_menu

    /**
     * The values for shared preferences used by this activity.
     */
    object EurotokenPreferences {
        const val EUROTOKEN_SHARED_PREF_NAME = "eurotoken"
        const val DEMO_MODE_ENABLED = "demo_mode_enabled"
    }
}
