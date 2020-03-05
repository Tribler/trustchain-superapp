package nl.tudelft.trustchain.debug

import android.os.Bundle
import androidx.core.view.isVisible
import nl.tudelft.trustchain.common.DrawerActivity

class DebugActivity : DrawerActivity() {
    override val navigationGraph = R.navigation.nav_graph

    override val topLevelDestinationIds = setOf(R.id.debugFragment)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.bottomNavigation.isVisible = false

        binding.navView.setCheckedItem(R.id.nav_debug)
    }
}
