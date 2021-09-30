package nl.tudelft.trustchain.datavault

import android.os.Bundle
import nl.tudelft.trustchain.common.BaseActivity

class DataVaultMainActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_graph_datavault
    override val bottomNavigationMenu = R.menu.data_vault_menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}

