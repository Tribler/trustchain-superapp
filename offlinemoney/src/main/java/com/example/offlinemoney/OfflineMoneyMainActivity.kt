package com.example.offlinemoney

import nl.tudelft.trustchain.common.BaseActivity

class OfflineMoneyMainActivity : BaseActivity() {

    override val navigationGraph = R.navigation.nav_graph_offlinemoney
    override val bottomNavigationMenu = R.menu.offlinemoney_navigation_menu

    /**
     * The values for shared preferences used by this activity.
     */
    object OfflineTokenPreferences {
        const val OFFLINE_MONEY_SHARED_PREF_NAME = "offlinemoney"
        const val DEMO_MODE_ENABLED = "demo_mode_enabled"
    }
}
