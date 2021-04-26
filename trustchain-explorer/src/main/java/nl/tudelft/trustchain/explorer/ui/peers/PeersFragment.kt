package nl.tudelft.trustchain.explorer.ui.peers

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.explorer.R
import nl.tudelft.trustchain.explorer.databinding.FragmentPeersBinding

class PeersFragment : BaseFragment(R.layout.fragment_peers) {
    private val adapter = ItemAdapter()

    private val binding by viewBinding(FragmentPeersBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter.registerRenderer(
            PeerItemRenderer {
                findNavController().navigate(
                    PeersFragmentDirections.actionPeersFragmentToBlocksFragment(
                        it.peer.publicKey.keyToBin().toHex()
                    )
                )
            }
        )

        adapter.registerRenderer(
            AddressItemRenderer {
                // NOOP
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayout.VERTICAL
            )
        )

        loadNetworkInfo()
    }

    private fun loadNetworkInfo() {
        lifecycleScope.launchWhenStarted {
            while (isActive) {
                val demoCommunity = getDemoCommunity()
                val peers = demoCommunity.getPeers()

                val discoveredAddresses = demoCommunity.network
                    .getWalkableAddresses(demoCommunity.serviceId)

                val discoveredBluetoothAddresses = demoCommunity.network
                    .getNewBluetoothPeerCandidates()
                    .map { it.address }

                val peerItems = peers.map {
                    PeerItem(
                        it
                    )
                }

                val addressItems = discoveredAddresses.map { address ->
                    val contacted = demoCommunity.discoveredAddressesContacted[address]
                    AddressItem(
                        address,
                        null,
                        contacted
                    )
                }

                val bluetoothAddressItems = discoveredBluetoothAddresses.map { address ->
                    AddressItem(
                        address,
                        null,
                        null
                    )
                }

                val items = peerItems + bluetoothAddressItems + addressItems

                adapter.updateItems(items)
                binding.txtCommunityName.text = demoCommunity.javaClass.simpleName
                binding.txtPeerCount.text = resources.getQuantityString(
                    R.plurals.x_peers, peers.size,
                    peers.size
                )
                val textColorResId = if (peers.isNotEmpty()) R.color.green else R.color.red
                val textColor = ResourcesCompat.getColor(resources, textColorResId, null)
                binding.txtPeerCount.setTextColor(textColor)
                binding.imgEmpty.isVisible = items.isEmpty()

                delay(1000)
            }
        }
    }
}
