package nl.tudelft.ipv8.android.demo.ui.peers

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
import nl.tudelft.ipv8.android.demo.R
import nl.tudelft.ipv8.android.demo.databinding.ActivityMainBinding
import nl.tudelft.ipv8.android.demo.util.viewBinding

val logger = KotlinLogging.logger {}

class MainActivity : AppCompatActivity() {
    private val binding by viewBinding(ActivityMainBinding::inflate)

    private val navController by lazy {
        findNavController(R.id.navHostFragment)
    }

    private val appBarConfiguration by lazy {
        val topLevelDestinationIds = setOf(R.id.peersFragment, R.id.usersFragment,
            R.id.latestBlocksFragment, R.id.myChainFragment, R.id.debugFragment)
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
