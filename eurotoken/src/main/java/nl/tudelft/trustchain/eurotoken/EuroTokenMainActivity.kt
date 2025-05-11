package nl.tudelft.trustchain.eurotoken

import android.os.Bundle
import nl.tudelft.trustchain.common.BaseActivity
import nl.tudelft.trustchain.eurotoken.benchmarks.UsageLogger

class EuroTokenMainActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_graph_eurotoken
    override val bottomNavigationMenu = R.menu.eurotoken_navigation_menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize UsageLogger
        UsageLogger.initialize(applicationContext)
    }

    /**
     * The values for shared preferences used by this activity.
     */
    object EurotokenPreferences {
        const val EUROTOKEN_SHARED_PREF_NAME = "eurotoken"
        const val DEMO_MODE_ENABLED = "demo_mode_enabled"
    }
}
