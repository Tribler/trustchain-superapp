package nl.tudelft.trustchain.currencyii.ui.peers

import mu.KotlinLogging
import nl.tudelft.trustchain.common.BaseActivity
import nl.tudelft.trustchain.currencyii.R

val logger = KotlinLogging.logger {}

class MainActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_graph
}
