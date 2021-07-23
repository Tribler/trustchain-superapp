package nl.tudelft.trustchain.valuetransfer

import nl.tudelft.trustchain.common.BaseActivity
import nl.tudelft.trustchain.valuetransfer.db.IdentityStore

class ValueTransferMainActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_graph_valuetransfer
//    override val bottomNavigationMenu = R.menu.valuetransfer_navigation_menu

    fun setActionBarTitle(title: String?) {
        supportActionBar!!.title = title
    }

    fun toggleActionBar(state: Boolean) {
        supportActionBar!!.setDisplayHomeAsUpEnabled(state)
        supportActionBar!!.setHomeButtonEnabled(state)
    }


}
