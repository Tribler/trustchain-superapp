package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_blockchain_download.*
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.coin.WalletManagerAndroid
import nl.tudelft.trustchain.currencyii.ui.BaseFragment
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.RegTestParams
import org.bitcoinj.params.TestNet3Params
import kotlin.concurrent.thread

/**
 * A simple [Fragment] subclass.
 * Use the [BlockchainDownloadFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BlockchainDownloadFragment() : BaseFragment(R.layout.fragment_blockchain_download) {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // TODO: The routing is cleaner to do via the previously displayed fragment.
        bitcoin_progress_continue.setOnClickListener {
            val navController = findNavController()
            val args = BlockchainDownloadFragmentArgs.fromBundle(requireArguments())
            when (args.parent) {
                -1 -> {
                    // Default value
                    Log.i("Coin", "Default value, navigating to My DAOs.")
                    navController.navigate(BlockchainDownloadFragmentDirections.actionBlockchainDownloadFragmentToMyDAOsFragment())
                }
                R.id.myDAOsFragment -> {
                    Log.i("Coin", "Navigating to My DAOs.")
                    navController.navigate(BlockchainDownloadFragmentDirections.actionBlockchainDownloadFragmentToMyDAOsFragment())
                }
                R.id.bitcoinFragment -> {
                    Log.i("Coin", "Navigating to My Wallet.")
                    navController.navigate(BlockchainDownloadFragmentDirections.actionBlockchainDownloadFragmentToBitcoinFragment())
                }
                else -> {
                    // Else just navigate to whatever was passed.
                    Log.i("Coin", "Navigating to ${args.parent}.")
                    navController.navigate(args.parent)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val fragment = inflater.inflate(R.layout.fragment_blockchain_download, container, false)
        if (WalletManagerAndroid.isInitialized()) {
            fragment.findViewById<TextView>(R.id.bitcoin_download_percentage).text =
                "${WalletManagerAndroid.getInstance().progress}%"
            fragment.findViewById<ProgressBar>(R.id.bitcoin_download_progress).progress =
                WalletManagerAndroid.getInstance().progress
            val networkName = when (WalletManagerAndroid.getInstance().params) {
                RegTestParams.get() -> "RegTest"
                TestNet3Params.get() -> "TestNet"
                MainNetParams.get() -> "MainNet"
                else -> return null
            }
            fragment.findViewById<TextView>(R.id.downloading_chain_tv).text = "Please wait while the chain from $networkName is downloading. "
            thread {
                // TODO: find a better way of handling uninitialized wallet managers while not stopping the while loop
                while (WalletManagerAndroid.getInstance().progress < 100) {
                    Thread.sleep(500)
                    if (!WalletManagerAndroid.isInitialized()) {
                        break
                    }
                    fragment.findViewById<TextView>(R.id.bitcoin_download_percentage).text =
                        "${WalletManagerAndroid.getInstance().progress}%"
                    fragment.findViewById<ProgressBar>(R.id.bitcoin_download_progress).progress =
                        WalletManagerAndroid.getInstance().progress
                }
                fragment.findViewById<TextView>(R.id.bitcoin_download_percentage).text =
                    "Fully Synced!"
                fragment.findViewById<Button>(R.id.bitcoin_progress_continue).text =
                    "Continue"
            }
        }
        hideNavBar()
        return fragment
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment BlockchainDownloading.
         */
        @JvmStatic
        fun newInstance() = BlockchainDownloadFragment()
    }
}
