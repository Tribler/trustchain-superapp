package nl.tudelft.trustchain.detoks

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.navigation.Navigation
import nl.tudelft.ipv8.IPv8
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.detoks.databinding.FragmentPeerConnectionBinding

/**
 * Fragment for selecting a peer to send tokens to
 */
class PeerConnectionFragment : BaseFragment(R.layout.fragment_peer_connection) {

    private val binding by viewBinding(FragmentPeerConnectionBinding::bind)

    private lateinit var ipv8: IPv8
    private lateinit var transactionEngine: DeToksTransactionEngine
    private lateinit var trustchainCommunity : TrustChainCommunity

    private lateinit var adapter : PeerAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Get communities and services
        ipv8 = IPv8Android.getInstance()
        transactionEngine = ipv8.getOverlay()!!
        trustchainCommunity = ipv8.getOverlay()!!

        // Set the list adapter for the ListView and add on click listeners
        adapter = PeerAdapter(requireActivity(), getPeers())

        binding.peerListview.isClickable = true
        binding.peerListview.adapter = adapter
        binding.peerListview.setOnItemClickListener() { _, _, position, _ ->
            val peer = adapter.getItem(position)
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Peer ${position}")
            builder.setMessage("Are you sure you want to connect to peer: ${peer.address}")

            // If we select a peer, return to the benchmark fragment
            builder.setPositiveButton("Yes") { _, _ ->
                transactionEngine.setPeer(peer)
                val navController = Navigation.findNavController(view)
                navController.navigate(R.id.action_return_to_benchmark_fragment)
            }
            builder.setNegativeButton("No") { _, _ ->
                // Do nothing
            }
            val dialog= builder.create()
            dialog.show()
        }

        // Set the update list button on click listener
        binding.peerlistUpdate.setOnClickListener {
            adapter.clear()
            for (peer in getPeers()){
                adapter.insert(peer, adapter.count)
            }
        }
    }

    /**
     * Returns a list of peers found in the TransactionEngine TrustChainCommunity
     */
    private fun getPeers(): ArrayList<Peer> {
        var peers = ArrayList<Peer>()
        peers.add(transactionEngine.myPeer)
        peers.addAll(transactionEngine.getPeers() as ArrayList<Peer>)
        return peers
    }
}
