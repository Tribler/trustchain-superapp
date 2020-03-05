package nl.tudelft.trustchain.explorer.ui

import android.os.Bundle
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import nl.tudelft.trustchain.common.DrawerActivity
import nl.tudelft.trustchain.explorer.R

class TrustChainExplorerActivity : DrawerActivity() {
    override val navigationGraph = R.navigation.nav_graph_explorer

    override val topLevelDestinationIds = setOf(R.id.peersFragment, R.id.usersFragment,
        R.id.latestBlocksFragment, R.id.myChainFragment, R.id.debugFragment)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup bottom navigation
        binding.bottomNavigation.inflateMenu(R.menu.bottom_navigation_menu)

        binding.navView.setCheckedItem(R.id.nav_trustchain_explorer)
    }
}
