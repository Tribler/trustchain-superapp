package nl.tudelft.trustchain.common

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.NavigationRes
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import nl.tudelft.trustchain.common.databinding.ActivityDrawerBinding
import nl.tudelft.trustchain.common.util.viewBinding

abstract class DrawerActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    protected val binding by viewBinding(ActivityDrawerBinding::inflate)

    private val appBarConfiguration: AppBarConfiguration by lazy {
        AppBarConfiguration(topLevelDestinationIds, binding.drawerLayout)
    }

    /**
     * The resource ID of the navigation graph.
     */
    protected abstract val navigationGraph: Int

    /**
     * Destination IDs of top-level destinations for which the back button should not be shown.
     */
    protected abstract val topLevelDestinationIds: Set<Int>

    protected val navController by lazy {
        findNavController(R.id.navHostFragment)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        navController.setGraph(navigationGraph)

        // Setup ActionBar
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Setup drawer navigation
        binding.navView.setNavigationItemSelectedListener(this)

        // Setup bottom navigation
        binding.bottomNavigation.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val app = (application as OnNavigationItemSelectedListener)
        return app.onNavigationItemSelected(this, item)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                val isOpen = binding.drawerLayout.isDrawerOpen(binding.navView)
                if (!onSupportNavigateUp() || isOpen) {
                    if (isOpen) {
                        binding.drawerLayout.closeDrawer(binding.navView)
                    } else {
                        binding.drawerLayout.openDrawer(binding.navView)
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
