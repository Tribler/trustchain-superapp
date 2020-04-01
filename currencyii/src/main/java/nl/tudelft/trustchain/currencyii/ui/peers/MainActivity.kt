package nl.tudelft.trustchain.currencyii.ui.peers

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.common.BaseActivity
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.databinding.ActivityMainBinding

val logger = KotlinLogging.logger {}

class MainActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_graph
    private val binding by viewBinding(ActivityMainBinding::inflate)

    private val navController by lazy {
        findNavController(R.id.navHostFragment)
    }

    private val appBarConfiguration by lazy {
        val topLevelDestinationIds = setOf(R.id.peersFragment, R.id.usersFragment,
            R.id.latestBlocksFragment, R.id.myChainFragment, R.id.debugFragment, R.id.bitcoinFragment)
        AppBarConfiguration(topLevelDestinationIds)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        // Setup ActionBar
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Setup bottom navigation
        binding.bottomNavigation.setupWithNavController(navController)

        lifecycleScope.launch {
            while (isActive) {

                delay(1000)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
