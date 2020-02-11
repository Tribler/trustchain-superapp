package nl.tudelft.ipv8.android.demo.ui.peers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.fragment_peers.*
import kotlinx.coroutines.*
import nl.tudelft.ipv8.android.demo.DemoCommunity
import nl.tudelft.ipv8.android.demo.R
import nl.tudelft.ipv8.android.demo.ui.BaseFragment
import nl.tudelft.ipv8.util.toHex

class PeersFragment : BaseFragment() {
    private val adapter = ItemAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter.registerRenderer(PeerItemRenderer {
            findNavController().navigate(
                PeersFragmentDirections.actionPeersFragmentToBlocksFragment(
                    it.peer.publicKey.keyToBin().toHex()
                )
            )
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_peers, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.addItemDecoration(DividerItemDecoration(context, LinearLayout.VERTICAL))

        loadNetworkInfo()
    }

    private fun loadNetworkInfo() {
        lifecycleScope.launchWhenStarted {
            val overlays = ipv8.getOverlays()

            for (overlay in overlays) {
                logger.debug(overlay.javaClass.simpleName + ": " + overlay.getPeers().size + " peers")
            }

            val demoCommunity = overlays.find { it is DemoCommunity }
                ?: throw IllegalStateException("DemoCommunity is not configured")
            val peers = demoCommunity.getPeers()

            val items = peers.map { PeerItem(it) }
            adapter.updateItems(items)
            txtCommunityName.text = demoCommunity.javaClass.simpleName
            txtPeerCount.text = resources.getQuantityString(
                R.plurals.x_peers, peers.size,
                peers.size)
            imgEmpty.isVisible = peers.isEmpty()

            delay(1000)
            loadNetworkInfo()
        }
    }
}
