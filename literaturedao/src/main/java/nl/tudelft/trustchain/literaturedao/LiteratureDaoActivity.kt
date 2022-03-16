package nl.tudelft.trustchain.literaturedao
import android.util.Log
import nl.tudelft.trustchain.common.BaseActivity
import nl.tudelft.trustchain.literaturedao.controllers.KeywordExtractor

open class LiteratureDaoActivity : BaseActivity() {
    override val navigationGraph: Int = R.navigation.nav_literaturedao
    override val bottomNavigationMenu: Int
        get() = super.bottomNavigationMenu


    override fun onStart() {
        super.onStart()

        Log.d("litdao", "starting ...")

        KeywordExtractor().actualImplementation("hello please banana I have a person")
    }


}
