package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.databinding.FragmentJoinNetworkBinding
import nl.tudelft.trustchain.currencyii.sharedWallet.SWJoinBlockTransactionData
import nl.tudelft.trustchain.currencyii.ui.BaseFragment

/**
 * A simple [Fragment] subclass.
 * Use the [MySharedWalletFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MySharedWalletFragment : BaseFragment(R.layout.fragment_my_shared_wallets) {
    private var _binding: FragmentJoinNetworkBinding? = null
    private val binding get() = _binding!!

    private fun initListView() {
        val sharedWalletBlocks = getCoinCommunity().fetchLatestJoinedSharedWalletBlocks()
        val publicKey = getTrustChainCommunity().myPeer.publicKey.keyToBin().toHex()
        val adaptor =
            SharedWalletListAdapter(this, sharedWalletBlocks, publicKey, "Click to enter wallet")
        binding.listView.adapter = adaptor
        binding.listView.setOnItemClickListener { _, view, position, id ->
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
        _binding = FragmentJoinNetworkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = MySharedWalletFragment()
    }
}
