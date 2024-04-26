package nl.tudelft.trustchain.currencyii.ui.bitcoin


import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.BlockListener
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.messaging.EndpointAggregator
import nl.tudelft.ipv8.messaging.EndpointListener
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.currencyii.CoinCommunity
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.databinding.FragmentDebugDashboardBinding
import nl.tudelft.trustchain.currencyii.ui.BaseFragment
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Calendar
import java.util.Calendar.*
import java.util.Collections
import java.util.Locale


/**
 * A simple [Fragment] subclass.
 * Use the [DebugDashboardFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DebugDashboardFragment : BaseFragment(R.layout.fragment_debug_dashboard) {
    @Suppress("ktlint:standard:property-naming") // False positive
    private var _binding: FragmentDebugDashboardBinding? = null
    private val binding get() = _binding!!
    private var adapter: PeerListAdapter? = null
    private var ipv8 = IPv8Android.getInstance()

    private var mainHandler: Handler? = null;

    private var peers: List<Peer>? = null;



    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initDebugDashboardView()
    }

    private val updateTextTask = object : Runnable {
        override fun run() {
            getPeersAndUpdateUI()
            mainHandler!!.postDelayed(this, 5000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        showNavBar()
        _binding = FragmentDebugDashboardBinding.inflate(inflater, container, false)

        mainHandler = Handler(Looper.getMainLooper())
        mainHandler!!.post(updateTextTask)
        return binding.root
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initDebugDashboardView() {
        this.peers = ipv8.getOverlay<CoinCommunity>()!!.getPeers();


        adapter = PeerListAdapter(
            this@DebugDashboardFragment,
            this.peers!!
        )

        binding.listView.adapter = adapter;
        binding.myLanIpv4.text = ipv8.getOverlay<CoinCommunity>()!!.myEstimatedLan.ip;
        binding.myWanIpv4.text = ipv8.getOverlay<CoinCommunity>()!!.myEstimatedWan.ip;
        binding.myPublicKey.text = ipv8.myPeer.publicKey.toString()
    }

    private fun getPeersAndUpdateUI() {
        this.peers = ipv8.getOverlay<CoinCommunity>()!!.getPeers();
        adapter!!.updateItems(this.peers!!)
        adapter!!.notifyDataSetChanged();
    }
    companion object {
        @JvmStatic
        fun newInstance() = DebugDashboardFragment()
    }

}
