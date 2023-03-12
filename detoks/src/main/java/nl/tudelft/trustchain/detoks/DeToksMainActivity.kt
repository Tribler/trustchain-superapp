package nl.tudelft.trustchain.detoks

import android.os.Bundle
import android.widget.TextView
import androidx.preference.PreferenceManager
import nl.tudelft.ipv8.android.keyvault.AndroidCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.BaseActivity
import java.security.PrivateKey


class DeToksActivity : BaseActivity() {
    override val navigationGraph = R.navigation.nav_graph_detoks


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //val actionBar = supportActionBar
        //actionBar!!.hide()
    }

    fun showBootstraps() {
        println("pls")
    }

}
