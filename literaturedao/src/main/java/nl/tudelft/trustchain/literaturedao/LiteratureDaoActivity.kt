package nl.tudelft.trustchain.literaturedao
import nl.tudelft.trustchain.common.BaseActivity

open class LiteratureDaoActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_literaturedao
    override val bottomNavigationMenu = R.menu.literature_navigation_menu

//    override fun onStart() {
//        super.onStart()
//
//        Log.d("litdao", "starting ...")
//
//        KeywordExtractor().actualImplementation("hello please banana I have a person")
//    }

}
