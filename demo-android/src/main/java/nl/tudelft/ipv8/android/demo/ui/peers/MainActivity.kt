package nl.tudelft.ipv8.android.demo.ui.peers

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import kotlinx.android.synthetic.main.activity_main.*
import mu.KotlinLogging
import nl.tudelft.ipv8.android.demo.R

val logger = KotlinLogging.logger {}

class MainActivity : AppCompatActivity() {
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

        setContentView(R.layout.activity_main)

        // Setup ActionBar
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Setup bottom navigation
        bottomNavigation.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
