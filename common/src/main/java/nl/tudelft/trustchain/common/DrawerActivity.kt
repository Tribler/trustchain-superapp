package nl.tudelft.trustchain.common

import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import nl.tudelft.trustchain.common.databinding.ActivityDrawerBinding
import nl.tudelft.trustchain.common.util.viewBinding

abstract class DrawerActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private val binding by viewBinding(ActivityDrawerBinding::inflate)

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

    /**
     * The ID of the menu item in the drawer navigation.
     */
    protected abstract val drawerNavigationItem: Int

    /**
     * The resource ID of the bottom menu if it should be shown.
     */
    @MenuRes
    protected open val bottomNavigationMenu: Int = 0

    private val navController by lazy {
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
        binding.navView.setCheckedItem(drawerNavigationItem)

        // Setup bottom navigation
        binding.bottomNavigation.setupWithNavController(navController)
        binding.bottomNavigation.isVisible = bottomNavigationMenu > 0
        if (bottomNavigationMenu > 0) {
            binding.bottomNavigation.inflateMenu(bottomNavigationMenu)
        }
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
