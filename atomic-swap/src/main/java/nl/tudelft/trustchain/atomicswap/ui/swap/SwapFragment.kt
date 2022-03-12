package nl.tudelft.trustchain.atomicswap.ui.swap

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.fragment_peers.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.atomicswap.AtomicSwapCommunity
import nl.tudelft.trustchain.atomicswap.R
import nl.tudelft.trustchain.atomicswap.databinding.FragmentPeersBinding
import nl.tudelft.trustchain.atomicswap.ui.peers.AddressItem
import nl.tudelft.trustchain.atomicswap.ui.peers.PeerItem
import nl.tudelft.trustchain.common.ui.BaseFragment
import androidx.core.view.isVisible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nl.tudelft.trustchain.atomicswap.ui.wallet.WalletHolder


class SwapFragment : BaseFragment(R.layout.fragment_peers) {
    private val adapter = ItemAdapter()
    val atomicSwapCommunity = IPv8Android.getInstance().getOverlay<AtomicSwapCommunity>()!!


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//
//        setContentView(R.layout.fragment_peers)
//
//        adapter.registerRenderer(PeerItemRenderer {
//            // NOOP
//        })
//
//        adapter.registerRenderer(AddressItemRenderer {
//            // NOOP
//        })
//
//        recyclerView.adapter = adapter
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        recyclerView.addItemDecoration(DividerItemDecoration(this, LinearLayout.VERTICAL))
//

        loadNetworkInfo()
        configureAtomicSwapCallbacks()
        //receiveGossips()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding =  FragmentPeersBinding.inflate(inflater, container, false)
        binding.button.setOnClickListener {
            lifecycleScope.launchWhenStarted {
                atomicSwapCommunity.broadcastTradeOffer(1,1.0)
            }
        }
        return binding.root
    }

    private fun configureAtomicSwapCallbacks(){

        atomicSwapCommunity.setOnTrade { trade, peer ->

            lifecycleScope.launch(Dispatchers.Main) {

                val alertDialogBuilder = AlertDialog.Builder(this@SwapFragment.requireContext())
                alertDialogBuilder.setTitle("Received Trade Offer")
                alertDialogBuilder.setMessage(trade.toString())
                alertDialogBuilder.setPositiveButton(android.R.string.yes) { _, _ ->

                    val freshKey = WalletHolder.bitcoinWallet.freshReceiveKey()
                    val freshKeyString  = freshKey.pubKey.toString()
                    atomicSwapCommunity.sendAcceptMessgae(peer, trade.offerId, freshKeyString)
                }
                alertDialogBuilder.setCancelable(true)
                alertDialogBuilder.show()
            }
        }

        atomicSwapCommunity.setOnAccept {
            lifecycleScope.launch(Dispatchers.Main) {

                val alertDialogBuilder = AlertDialog.Builder(this@SwapFragment.requireContext())
                alertDialogBuilder.setTitle("Received Accept")
                alertDialogBuilder.setMessage(it.toString())
                alertDialogBuilder.setCancelable(true)
                alertDialogBuilder.show()
            }
        }
    }

    private fun loadNetworkInfo() {
        lifecycleScope.launchWhenStarted {
            while (isActive) {
                val peers = atomicSwapCommunity.getPeers()

                val discoveredAddresses = atomicSwapCommunity.network
                    .getWalkableAddresses(atomicSwapCommunity.serviceId)

                val discoveredBluetoothAddresses = atomicSwapCommunity.network
                    .getNewBluetoothPeerCandidates()
                    .map { it.address }

                val peerItems = peers.map {
                    PeerItem(
                        it
                    )
                }

                val addressItems = discoveredAddresses.map { address ->
                    val contacted = atomicSwapCommunity.discoveredAddressesContacted[address]
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

                for (peer in peers) {
                    Log.d("AtomicSwapCommunity", "FOUND PEER with id " + peer.mid)
                }


                adapter.updateItems(items)
                txtCommunityName.text = atomicSwapCommunity.javaClass.simpleName
                txtPeerCount.text = "${peers.size} peers"
                val textColorResId = if (peers.isNotEmpty()) R.color.green else R.color.red
                val textColor = ResourcesCompat.getColor(resources, textColorResId, null)
                txtPeerCount.setTextColor(textColor)
                imgEmpty.isVisible = items.isEmpty()

                delay(3000)
            }
        }
    }
}
