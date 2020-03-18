package nl.tudelft.ipv8.android.demo.ui.bitcoin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import nl.tudelft.ipv8.android.demo.R
import nl.tudelft.ipv8.android.demo.coin.WalletManagerAndroid
import nl.tudelft.ipv8.android.demo.ui.BaseFragment
import kotlin.IllegalArgumentException

/**
 * A simple [Fragment] subclass.
 * Use the [BitcoinFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LandingBitcoinFragment : BaseFragment(R.layout.fragment_landing_bitcoin), BitcoinViewController {

    /**
     * Loads the view fragments and map them to strings as identifiers.
     */
    private val bitcoinViews = mapOf<String, Fragment>(
        "BitcoinFragment" to BitcoinFragment.newInstance(this),
        "JoinNetworkFragment" to JoinNetworkFragment.newInstance(this),
        "CreateSWFragment" to CreateSWFragment.newInstance(this),
        "BlockchainDownloading" to BlockchainDownloading.newInstance(this)
    )

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        loadInitialView()
    }

    private fun loadInitialView() {
        if (WalletManagerAndroid.getInstance().isDownloading) {
            showView("BlockchainDownloading")
        } else {
            showView("BitcoinFragment")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_landing_bitcoin, container, false)
    }

    /**
     * BitcoinViewController function that handles switching views.
     * Possible bitcoinViewName strings are defined in the map `bitcoinViews` of this class.
     */
    override fun showView(bitcoinViewName: String) {
        val fragment = bitcoinViews[bitcoinViewName]
            ?: throw IllegalArgumentException("$bitcoinViewName does not exist. Choose from ${bitcoinViews.keys}")

        val transaction = parentFragmentManager.beginTransaction()
        transaction.replace(R.id.landing_bitcoin_container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun showDefaultView() {
        showView("BitcoinFragment")
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment bitcoinFragment.
         */
        @JvmStatic
        fun newInstance() = LandingBitcoinFragment()
    }
}
