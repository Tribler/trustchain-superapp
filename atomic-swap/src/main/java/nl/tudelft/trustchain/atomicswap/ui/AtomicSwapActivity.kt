package nl.tudelft.trustchain.atomicswap.ui

import nl.tudelft.trustchain.atomicswap.R
import nl.tudelft.trustchain.common.BaseActivity

class AtomicSwapActivity : BaseActivity() {
    override val navigationGraph get() = R.navigation.atomic_swap_navigation_graph
    override val bottomNavigationMenu get() = R.menu.atomic_swap_menu
}
