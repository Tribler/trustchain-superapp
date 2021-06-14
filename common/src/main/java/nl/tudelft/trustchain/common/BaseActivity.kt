package nl.tudelft.trustchain.common

import android.os.Bundle
import android.view.View
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import nl.tudelft.trustchain.common.databinding.ActivityBaseBinding
import nl.tudelft.trustchain.common.util.viewBinding

abstract class BaseActivity : AppCompatActivity() {
    private val binding by viewBinding(ActivityBaseBinding::inflate)

    protected open val appBarConfiguration: AppBarConfiguration by lazy {
        AppBarConfiguration(setOf())
    }

    protected lateinit var bottomNavigation: BottomNavigationView

    /**
     * The resource ID of the navigation graph.
     */
    protected abstract val navigationGraph: Int

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

        bottomNavigation = binding.bottomNavigation
        navController.setGraph(navigationGraph)

        // Setup ActionBar
        println("---")
        println(navController)
        println(appBarConfiguration)
        println("---")
        setupActionBarWithNavController(navController, appBarConfiguration)

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
}
