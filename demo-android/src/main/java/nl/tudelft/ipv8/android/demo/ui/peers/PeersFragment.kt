package nl.tudelft.ipv8.android.demo.ui.peers

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.*
import nl.tudelft.ipv8.android.demo.R
import nl.tudelft.ipv8.android.demo.databinding.FragmentPeersBinding
import nl.tudelft.ipv8.android.demo.ui.BaseFragment
import nl.tudelft.ipv8.android.demo.util.viewBinding
import nl.tudelft.ipv8.util.toHex

class PeersFragment : BaseFragment(R.layout.fragment_peers) {
    private val adapter = ItemAdapter()

    private val binding by viewBinding(FragmentPeersBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter.registerRenderer(PeerItemRenderer {
            findNavController().navigate(
                PeersFragmentDirections.actionPeersFragmentToBlocksFragment(
                    it.peer.publicKey.keyToBin().toHex()
                )
            )
        })

        adapter.registerRenderer(AddressItemRenderer {
            // NOOP
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.addItemDecoration(DividerItemDecoration(context, LinearLayout.VERTICAL))

        loadNetworkInfo()
    }

    private fun loadNetworkInfo() {
        lifecycleScope.launchWhenStarted {
            while (isActive) {
                val overlays = getIpv8().overlays

                for ((_, overlay) in overlays) {
                    logger.debug(overlay.javaClass.simpleName + ": " + overlay.getPeers().size + " peers")
                }

                val demoCommunity = getDemoCommunity()
                val peers = demoCommunity.getPeers()

                val discoveredAddresses = demoCommunity.network
                    .getWalkableAddresses(demoCommunity.serviceId)

                val peerItems = peers.map { PeerItem(it) }

                val addressItems = discoveredAddresses.map { address ->
                    val contacted = demoCommunity.discoveredAddressesContacted[address]
                    AddressItem(address, null, contacted)
                }

                val items = peerItems + addressItems

                adapter.updateItems(items)
                binding.txtCommunityName.text = demoCommunity.javaClass.simpleName
                binding.txtPeerCount.text = resources.getQuantityString(
                    R.plurals.x_peers, peers.size,
                    peers.size
                )
                binding.imgEmpty.isVisible = items.isEmpty()

                delay(1000)
            }
        }
    }
}
