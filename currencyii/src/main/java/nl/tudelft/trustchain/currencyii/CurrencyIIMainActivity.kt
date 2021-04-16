package nl.tudelft.trustchain.currencyii

import android.util.Log
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import nl.tudelft.trustchain.common.BaseActivity
import org.bitcoinj.core.ECKey
import org.bouncycastle.math.ec.ECPoint

lateinit var NONCE_KEY: Pair<ECKey, ECPoint>

class CurrencyIIMainActivity : BaseActivity() {
    private var topLevelDestinationIds = setOf(R.id.blockchainDownloadFragment, R.id.daoLoginChoice)

    override val navigationGraph = R.navigation.nav_graph
    override val bottomNavigationMenu = R.menu.currencyii_bottom_navigation_menu

    override val appBarConfiguration by lazy {
        AppBarConfiguration(topLevelDestinationIds)
    }

    override fun onBackPressed() {
        val currentFragment = findNavController(R.id.navHostFragment).currentDestination
        val currentDestinationId = currentFragment?.id
        if (topLevelDestinationIds.contains(currentDestinationId)) {
            // Do not allow hardware back press on any top level destinations.
            Log.i("Coin", "Hardware back press not allowed on top level destinations.")
        } else {
            super.onBackPressed()
        }
    }

    fun addTopLevelDestinationId(id: Int) {
        val topLevelDestinationIdsList = topLevelDestinationIds.toMutableList()
        if (!topLevelDestinationIdsList.contains(id)) {
            topLevelDestinationIdsList.add(id)
            topLevelDestinationIds = topLevelDestinationIdsList.toSet()
        }
    }

    fun removeTopLevelDestinationId(id: Int) {
        val topLevelDestinationIdsList = topLevelDestinationIds.toMutableList()
        if (topLevelDestinationIdsList.contains(id)) {
            topLevelDestinationIdsList.remove(id)
            topLevelDestinationIds = topLevelDestinationIdsList.toSet()
        }
    }
}
