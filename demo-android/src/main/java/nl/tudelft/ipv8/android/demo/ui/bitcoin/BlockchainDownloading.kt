package nl.tudelft.ipv8.android.demo.ui.bitcoin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import nl.tudelft.ipv8.android.demo.R
import nl.tudelft.ipv8.android.demo.coin.WalletManagerAndroid
import kotlin.concurrent.thread

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [BlockchainDownloading.newInstance] factory method to
 * create an instance of this fragment.
 */
class BlockchainDownloading : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val fragment = inflater.inflate(R.layout.fragment_blockchain_downloading, container, false)
        fragment.findViewById<TextView>(R.id.bitcoin_download_percentage).text = "${WalletManagerAndroid.getInstance().progress.toString()}%"
        fragment.findViewById<ProgressBar>(R.id.bitcoin_download_progress).progress = WalletManagerAndroid.getInstance().progress
        thread {
            while (WalletManagerAndroid.getInstance().progress < 100) {
                Thread.sleep(500)
                fragment.findViewById<TextView>(R.id.bitcoin_download_percentage).text = "${WalletManagerAndroid.getInstance().progress}%"
                fragment.findViewById<ProgressBar>(R.id.bitcoin_download_progress).progress = WalletManagerAndroid.getInstance().progress
            }
        }
        return fragment
    }



    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment BlockchainDownloading.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            BlockchainDownloading().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
