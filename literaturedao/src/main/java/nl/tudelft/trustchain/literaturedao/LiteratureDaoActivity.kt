package nl.tudelft.trustchain.literaturedao
import nl.tudelft.trustchain.common.BaseActivity

open class LiteratureDaoActivity : BaseActivity() {
    override val navigationGraph: Int = R.navigation.nav_literaturedao
    override val bottomNavigationMenu: Int
        get() = super.bottomNavigationMenu
}
