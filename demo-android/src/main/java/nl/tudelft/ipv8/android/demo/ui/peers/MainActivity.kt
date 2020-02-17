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
import nl.tudelft.ipv8.android.demo.DemoApplication
import nl.tudelft.ipv8.android.demo.R
import nl.tudelft.ipv8.android.demo.service.IPv8Service
import nl.tudelft.ipv8.android.demo.startIfNotRunning

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

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
        }

        override fun onServiceDisconnected(name: ComponentName) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // Setup ActionBar
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Setup bottom navigation
        bottomNavigation.setupWithNavController(navController)
    }

    override fun onStart() {
        super.onStart()

        // Make sure IPv8 is running
        (application as DemoApplication).ipv8.startIfNotRunning(this)

        val serviceIntent = Intent(this, IPv8Service::class.java)
        bindService(serviceIntent, serviceConnection, 0)
    }

    override fun onStop() {
        unbindService(serviceConnection)
        super.onStop()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
