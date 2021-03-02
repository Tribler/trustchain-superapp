package nl.tudelft.trustchain.ssi.peers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.fragment_peers2.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.wallet.AttestationCommunity
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.ssi.R
import nl.tudelft.trustchain.ssi.databinding.FragmentPeers2Binding
import nl.tudelft.trustchain.ssi.dialogs.FireMissilesDialog

class Peers2Fragment : BaseFragment(R.layout.fragment_peers2) {

    private val adapterClients = ItemAdapter()
    private val adapterAuthorities = ItemAdapter()
    private val binding by viewBinding(FragmentPeers2Binding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapterClients.registerRenderer(
            PeerItemRenderer(
                {
                    FireMissilesDialog(it.peer).show(parentFragmentManager, this.tag)
                },
                {
                    copyToClipboard(it.peer)
                }
            )
        )

        adapterClients.registerRenderer(
            AddressItemRenderer {
                // NOOP
            }
        )

        adapterAuthorities.registerRenderer(
            AuthorityItemRenderer(
                {},
                {
                    RemoveAuthorityDialog(it, ::loadAuthorities).show(
                        parentFragmentManager,
                        this.tag
                    )
                }
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewClients.adapter = adapterClients
        binding.recyclerViewClients.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewClients.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayout.VERTICAL
            )
        )

        binding.recyclerViewAuthorities.adapter = adapterAuthorities
        binding.recyclerViewAuthorities.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewAuthorities.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayout.VERTICAL
            )
        )

        loadAuthorities()
        loadNetworkInfo()
    }

    private fun copyToClipboard(peer: Peer): Boolean {
        val clipboard =
            requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("RANDOM UUID", peer.address.toString())
        clipboard.setPrimaryClip(clip)

        val text = "Copied address to clipboard!"
        val duration = Toast.LENGTH_SHORT

        val toast = Toast.makeText(context, text, duration)
        toast.show()
        return true
    }

    private fun loadNetworkInfo() {
        lifecycleScope.launchWhenStarted {
            while (isActive) {
                val demoCommunity = IPv8Android.getInstance().getOverlay<AttestationCommunity>()!!
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
//                    val contacted = demoCommunity.discoveredAddressesContacted[address]
                    AddressItem(
                        address,
                        null,
                        null
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

                adapterClients.updateItems(items)
                txtCommunityName.text = demoCommunity.javaClass.simpleName
                txtPeerCount.text = "${peers.size} peers"
                val textColorResId = if (peers.isNotEmpty()) R.color.green else R.color.red
                val textColor = ResourcesCompat.getColor(resources, textColorResId, null)
                txtPeerCount.setTextColor(textColor)
                imgEmpty1.isVisible = items.isEmpty()

                delay(1000)
            }
        }
    }

    private fun loadAuthoritiesOnLoop() {
        lifecycleScope.launchWhenStarted {
            while (isActive) {
                loadAuthorities()
                delay(1500)
            }
        }
    }

    private fun loadAuthorities() {
        val authorities = IPv8Android.getInstance()
            .getOverlay<AttestationCommunity>()!!.trustedAuthorityManager.getAuthorities()
            .map { AuthorityItem(it.publicKey, it.hash, "lorem ipsum") }
        imgEmpty.isVisible = authorities.isEmpty()
        binding.txtAuthoritiesCount.text = "${authorities.size} authorities"
        adapterAuthorities.updateItems(authorities)
    }
}

class RemoveAuthorityDialog(val item: AuthorityItem, val callback: (() -> Unit) = { }) :
    DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            builder.setView(view)
                .setPositiveButton(
                    "Delete Authority",
                    DialogInterface.OnClickListener { _, _ ->
                        IPv8Android.getInstance()
                            .getOverlay<AttestationCommunity>()!!.trustedAuthorityManager.deleteTrustedAuthority(
                            item.publicKeyHash
                        )
                        Toast.makeText(
                            requireContext(),
                            "Successfully deleted authority",
                            Toast.LENGTH_LONG
                        ).show()
                        callback()
                    }
                )
                .setNegativeButton(
                    R.string.cancel,
                    DialogInterface.OnClickListener { _, _ ->
                        Toast.makeText(
                            requireContext(),
                            "Cancelled deletion",
                            Toast.LENGTH_LONG
                        ).show()
                        callback()
                    }
                )
                .setTitle("Delete Authority permanently?")
                .setIcon(R.drawable.ic_round_warning_amber_24)
                .setMessage("Note: this action cannot be undone.")
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
