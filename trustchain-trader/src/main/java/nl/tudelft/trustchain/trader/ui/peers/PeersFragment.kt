package nl.tudelft.trustchain.trader.ui.peers

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
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
import nl.tudelft.trustchain.trader.R
import nl.tudelft.trustchain.trader.databinding.FragmentPeersBinding

class PeersFragment : BaseFragment(R.layout.fragment_peers) {
    private val adapter = ItemAdapter()

    private val binding by viewBinding(FragmentPeersBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter.registerRenderer(
            PeerItemRenderer {
                val bundle = bundleOf("Public Key" to it.peer.publicKey.keyToBin().toHex())
                findNavController().navigate(R.id.action_peerFragment_to_transferFragment, bundle)
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
                val marketCommunity = getMarketCommunity()
                val peers = marketCommunity.getPeers()

                val discoveredAddresses = marketCommunity.network
                    .getWalkableAddresses(marketCommunity.serviceId)

                val peerItems = peers.map {
                    PeerItem(
                        it
                    )
                }

                val addressItems = discoveredAddresses.map { address ->
                    val contacted = marketCommunity.discoveredAddressesContacted[address]
                    AddressItem(
                        address,
                        null,
                        contacted
                    )
                }

                val items = peerItems + addressItems

                adapter.updateItems(items)
                binding.txtCommunityName.text = marketCommunity.javaClass.simpleName
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
