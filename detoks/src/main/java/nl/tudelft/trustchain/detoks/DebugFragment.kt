package nl.tudelft.trustchain.detoks

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.detoks.gossiper.NetworkSizeGossiper
import nl.tudelft.trustchain.common.ui.BaseFragment


class DebugFragment : BaseFragment(R.layout.fragment_debug) {

    private val deToksCommunity = IPv8Android.getInstance().getOverlay<DeToksCommunity>()!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val estimatedNetworkSizeTV = view.findViewById<TextView>(R.id.estimatedNetworkSizeTextView)
        val nbConnectedPeersTV = view.findViewById<TextView>(R.id.nbConnectedPeersTextView)
        val listConnectedPeersTV = view.findViewById<TextView>(R.id.listConnectedPeersTextView)

        fun updateDebugPage() {
            val estimatedNetworkSize = NetworkSizeGossiper.networkSizeEstimate
            estimatedNetworkSizeTV.text = getString(R.string.estimated_network_size, estimatedNetworkSize)

            val nbConnectedPeers = deToksCommunity.getPeers().size
            nbConnectedPeersTV.text = getString(R.string.nb_connected_peers, nbConnectedPeers)

            val connectedPeers = deToksCommunity.getPeers()
            var listConnectedPeers = ""
            for (peer in connectedPeers) {
                listConnectedPeers += "\n" + peer.mid
            }
            listConnectedPeersTV.text = getString(R.string.list_connected_peers, listConnectedPeers)
        }

        val handler = Handler((Looper.getMainLooper()))
        val runnable : Runnable = object : Runnable {
            override fun run() {
                if (isAdded)
                    updateDebugPage()
                handler.postDelayed(this, 2000)
            }
        }
        handler.postDelayed(runnable,2000)
    }
}
