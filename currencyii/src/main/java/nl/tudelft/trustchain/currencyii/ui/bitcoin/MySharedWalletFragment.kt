package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_join_network.*
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.sharedWallet.SWJoinBlockTransactionData
import nl.tudelft.trustchain.currencyii.ui.BaseFragment

/**
 * A simple [Fragment] subclass.
 * Use the [MySharedWalletFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MySharedWalletFragment : BaseFragment(R.layout.fragment_my_shared_wallets) {

    private fun initListView() {
        val sharedWalletBlocks = getCoinCommunity().fetchLatestJoinedSharedWalletBlocks()
        val publicKey = getTrustChainCommunity().myPeer.publicKey.keyToBin().toHex()
        val adaptor =
            SharedWalletListAdapter(this, sharedWalletBlocks, publicKey, "Click to enter wallet")
        list_view.adapter = adaptor
        list_view.setOnItemClickListener { _, view, position, id ->
            val block = sharedWalletBlocks[position]
            val blockData = SWJoinBlockTransactionData(block.transaction).getData()
            findNavController().navigate(
                MySharedWalletFragmentDirections.actionMySharedWalletsFragmentToSharedWalletTransaction(
                    blockData.SW_UNIQUE_ID,
                    blockData.SW_VOTING_THRESHOLD,
                    blockData.SW_ENTRANCE_FEE,
                    blockData.SW_TRUSTCHAIN_PKS.size,
                    block.calculateHash().toHex()
                )
            )
            Log.i("Coin", "Clicked: $view, $position, $id")
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initListView()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_my_shared_wallets, container, false)
    }

    companion object {
        @JvmStatic
        fun newInstance() = MySharedWalletFragment()
    }
}
