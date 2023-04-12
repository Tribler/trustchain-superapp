package nl.tudelft.trustchain.detoks

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.navigation.Navigation
import nl.tudelft.ipv8.IPv8
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.detoks.databinding.FragmentPeerConnectionBinding

class PeerConnectionFragment : BaseFragment(R.layout.fragment_peer_connection) {

    private val binding by viewBinding(FragmentPeerConnectionBinding::bind)

    private lateinit var ipv8: IPv8
    private lateinit var transactionEngine: DeToksTransactionEngine
    private lateinit var trustchainCommunity : TrustChainCommunity

    private lateinit var adapter : PeerAdapter

    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            adapter = PeerAdapter(requireActivity(), getPeers())
            handler.postDelayed(this, 1000)
        }
    }

    /**
     * Updates a list of blocks for debug purposes
     */

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ipv8 = IPv8Android.getInstance()
        transactionEngine = ipv8.getOverlay()!!
        trustchainCommunity = ipv8.getOverlay()!!

        // Update the list adapter to incorporate all peers
        adapter = PeerAdapter(requireActivity(), getPeers())
        binding.peerlistUpdate.setOnClickListener {
            adapter = PeerAdapter(requireActivity(), getPeers())
        }

        binding.peerListview.isClickable = true
        binding.peerListview.adapter = adapter
        binding.peerListview.setOnItemClickListener() { _, _, position, _ ->
            val peer = adapter.getItem(position)
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Peer ${position}")
            builder.setMessage("Are you sure you want to connect to peer: ${peer.address}")

            builder.setPositiveButton("Yes") { _, _ ->
                if (position == 0) {
                    transactionEngine.addPeer(peer, true)
                } else {
                    transactionEngine.addPeer(peer, false)
                }
                val navController = Navigation.findNavController(view)
                navController.navigate(R.id.action_return_to_benchmark_fragment)
            }
            builder.setNegativeButton("No") { _, _ ->
                // Do nothing
            }
            val dialog= builder.create()
            dialog.show()
        }
    }
    private fun getPeers(): ArrayList<Peer> {
        var peers = ArrayList<Peer>()
        peers.add(transactionEngine.myPeer)
        peers.addAll(transactionEngine.getPeers() as ArrayList<Peer>)
        return peers
    }

    override fun onResume() {
        super.onResume()
        Log.d("UPDATE_VIEWS", "Handler set")
        handler.postDelayed(runnable, 1000)
    }

    override fun onPause() {
        super.onPause()
        Log.d("UPDATE_VIEWS", "Handler stopped")
        handler.removeCallbacks(runnable)
    }
}
