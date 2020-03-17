package nl.tudelft.ipv8.android.demo.ui.bitcoin

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_join_network.*
import nl.tudelft.ipv8.android.demo.R
import nl.tudelft.ipv8.android.demo.ui.BaseFragment


/**
 * A simple [Fragment] subclass.
 * Use the [MySharedWalletFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MySharedWalletFragment(
    override val controller: BitcoinViewController
) : BitcoinView, BaseFragment(R.layout.fragment_my_shared_wallets) {

    private fun initListView() {
        val sharedWalletBlocks = getCoinCommunity().fetchLatestJoinedSharedWalletBlocks()
        val adaptor = SharedWalletListAdapter(this, sharedWalletBlocks)
        list_view.adapter = adaptor
        list_view.setOnItemClickListener { _, view, position, id ->
            controller.showSharedWalletTransactionView(sharedWalletBlocks[position])
            Log.i("Coin", "Clicked: $view, $position, $id")
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initListView()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_my_shared_wallets, container, false)
    }

    companion object {
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(controller: BitcoinViewController) = MySharedWalletFragment(controller)
    }
}
