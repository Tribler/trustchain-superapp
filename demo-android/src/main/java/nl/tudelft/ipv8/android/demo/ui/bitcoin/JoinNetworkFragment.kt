package nl.tudelft.ipv8.android.demo.ui.bitcoin

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_join_network.*
import nl.tudelft.ipv8.android.demo.CoinCommunity
import nl.tudelft.ipv8.android.demo.R
import nl.tudelft.ipv8.android.demo.ui.BaseFragment
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.toHex
import org.json.JSONObject

/**
 * A simple [Fragment] subclass.
 * Use the [BitcoinFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class JoinNetworkFragment (
    override val controller: BitcoinViewController
) : BitcoinView, BaseFragment(R.layout.fragment_join_network) {
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val sharedWalletPublicKeys = getCoinCommunity()
            .discoverSharedWalletsTrustchainPublicKeys()
            .map { pk ->
                getTrustChainCommunity().database.getLatest(pk, CoinCommunity.SW_JOIN_BLOCK)
                    ?: throw IllegalArgumentException("Shared Wallet block not found")
            }

        val adaptor = JoinNetworkListAdapter(this, sharedWalletPublicKeys)
        list_view.adapter = adaptor
        list_view.setOnItemClickListener { _, view, position, id ->
            Log.i("Coin", "Clicked: $view, $position, $id")
        }
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

class JoinNetworkListAdapter(private val context: BaseFragment, private val items: List<TrustChainBlock>): BaseAdapter() {
    override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
        val inflater = context.layoutInflater
        val view1 = inflater.inflate(R.layout.join_sw_row_data, null)

        var publicKeyTextView = view1.findViewById<TextView>(R.id.sw_id_item_t)
//        var blockDataTextView = view1.findViewById<TextView>(R.id.sw_data_tf)
        publicKeyTextView.text = items[p0].publicKey.toHex()
//        blockDataTextView.text = JSONObject(items[p0].transaction).toString()

        return view1
    }

    override fun getItem(p0: Int): Any {
        return items[p0]
    }

    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }

    override fun getCount(): Int {
        return items.size
    }
}
