package nl.tudelft.ipv8.android.demo.ui.bitcoin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_blockchain_download.*
import nl.tudelft.ipv8.android.demo.R
import nl.tudelft.ipv8.android.demo.coin.WalletManagerAndroid
import nl.tudelft.ipv8.android.demo.ui.BaseFragment
import kotlin.concurrent.thread


/**
 * A simple [Fragment] subclass.
 * Use the [BlockchainDownloadFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BlockchainDownloadFragment() : BaseFragment(R.layout.fragment_blockchain_download) {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        bitcoin_progress_continue.setOnClickListener {
            findNavController().navigate(BlockchainDownloadFragmentDirections.actionBlockchainDownloadFragmentToBitcoinFragment())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val fragment = inflater.inflate(R.layout.fragment_blockchain_download, container, false)
        fragment.findViewById<TextView>(R.id.bitcoin_download_percentage).text =
            "${WalletManagerAndroid.getInstance().progress.toString()}%"
        fragment.findViewById<ProgressBar>(R.id.bitcoin_download_progress).progress =
            WalletManagerAndroid.getInstance().progress
        thread {
            while (WalletManagerAndroid.getInstance().progress < 100) {
                Thread.sleep(500)
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
