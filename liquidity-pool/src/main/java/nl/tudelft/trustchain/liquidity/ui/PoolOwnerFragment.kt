package nl.tudelft.trustchain.liquidity.ui

import android.content.ClipData
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_pool_wallet.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.liquidity.R
import nl.tudelft.trustchain.liquidity.data.EuroTokenWallet
import org.bitcoinj.wallet.Wallet
import java.security.acl.Owner


/**
 * A simple [Fragment] subclass.
 * Use the [PoolOwnerFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PoolOwnerFragment : BaseFragment(R.layout.fragment_pool_owner) {

    private val transactionRepository by lazy {
        TransactionRepository(getIpv8().getOverlay()!!, GatewayStore.getInstance(requireContext()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val list: LinearLayout = requireView().findViewById<View>(R.id.list) as LinearLayout
        val euroWallet = EuroTokenWallet(transactionRepository, getIpv8().myPeer.publicKey);

        lifecycleScope.launchWhenStarted {

            while (isActive) {
                list.removeAllViews()
                val owners = euroWallet.getPoolOwners()
                for (owner in owners) {
                    val textView = TextView(requireContext())
                    textView.setText(owner)
                    list.addView(textView)
                }

                delay(1000)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pool_owner, container, false)
    }
}
