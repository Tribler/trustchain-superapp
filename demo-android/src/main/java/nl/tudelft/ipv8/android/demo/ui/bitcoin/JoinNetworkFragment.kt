package nl.tudelft.ipv8.android.demo.ui.bitcoin

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_join_network.*
import nl.tudelft.ipv8.android.demo.CoinCommunity
import nl.tudelft.ipv8.android.demo.R
import nl.tudelft.ipv8.android.demo.ui.BaseFragment
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock

/**
 * A simple [Fragment] subclass.
 * Use the [BitcoinFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class JoinNetworkFragment (
    override val controller: BitcoinViewController
) : BitcoinView, BaseFragment(R.layout.fragment_join_network) {
    private val tempBitcoinPk = ByteArray(2)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val sharedWalletBlocks = getCoinCommunity().discoverSharedWallets()
        val adaptor = SharedWalletListAdapter(this, sharedWalletBlocks)
        list_view.adapter = adaptor
        list_view.setOnItemClickListener { _, view, position, id ->
            joinSharedWalletClicked(sharedWalletBlocks[position])
            Log.i("Coin", "Clicked: $view, $position, $id")
        }
    }

    private fun joinSharedWalletClicked(block: TrustChainBlock) {
        getCoinCommunity().joinSharedWallet(block.calculateHash(), tempBitcoinPk)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_join_network, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment bitcoinFragment.
         */
        @JvmStatic
        fun newInstance(controller: BitcoinViewController) = JoinNetworkFragment(controller)
    }
}
