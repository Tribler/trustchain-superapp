package nl.tudelft.trustchain.valuetransfer.ui.walletoverview

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.peerchat.community.PeerChatCommunity
import nl.tudelft.trustchain.peerchat.db.PeerChatStore
import nl.tudelft.trustchain.peerchat.entity.ChatMessage
import nl.tudelft.trustchain.peerchat.ui.contacts.ContactItem
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentWalletContactsBinding
import nl.tudelft.trustchain.valuetransfer.ui.contacts.ChatItemRenderer
import nl.tudelft.trustchain.valuetransfer.ui.contacts.ContactsFragment

class WalletContactsFragment : BaseFragment(R.layout.fragment_wallet_contacts) {
    private val binding by viewBinding(FragmentWalletContactsBinding::bind)

    private val adapter = ItemAdapter()

    private val peerChatStore by lazy {
        PeerChatStore.getInstance(requireContext())
    }

    private val peers = MutableStateFlow<List<Peer>>(listOf())

    private val items: LiveData<List<Item>> by lazy {
        combine(peerChatStore.getContactsWithLastMessages(), peers) { contacts, peers ->
            Log.d("TESTJE", "SIZE: ${contacts.size} ${peers.size}")
            createItems(contacts, peers)
        }.asLiveData()
    }

    private fun getPeerChatCommunity(): PeerChatCommunity {
        return getIpv8().getOverlay()
            ?: throw java.lang.IllegalStateException("PeerChatCommunity is not configured")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter.registerRenderer(
            ChatItemRenderer(
                {
                    val args = Bundle()
                    args.putString(WalletContactsFragment.ARG_PUBLIC_KEY, it.publicKey.keyToBin().toHex())
                    args.putString(WalletContactsFragment.ARG_NAME, it.name)
                    findNavController().navigate(R.id.action_walletOverviewFragment_to_contactChatFragment, args)
                }, {
                    Log.d("TESTJE", "ON EXCHANGE CLICK")
                },
                0
            )
        )

        lifecycleScope.launchWhenResumed {
            while (isActive) {
                peers.value = getPeerChatCommunity().getPeers()
                delay(1000L)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvContactChats.adapter = adapter
        binding.rvContactChats.layoutManager = LinearLayoutManager(context)

        items.observe(
            viewLifecycleOwner,
            Observer {
                adapter.updateItems(it)
            }
        )
    }

    private fun createItems(
        contacts: List<Pair<Contact, ChatMessage?>>,
        peers: List<Peer>
    ): List<Item> {
        return contacts.filter { it.first.publicKey != getIpv8().myPeer.publicKey }
            .sortedByDescending { item ->
                item.second?.timestamp?.time
            }
            .filterIndexed { index, _ ->
                index < MAX_CHATS
            }
            .map { contactWithMessage ->
                val (contact, message) = contactWithMessage
                val peer = peers.find { it.mid == contact.mid }
                ContactItem(
                    contact,
                    message,
                    peer != null && !peer.address.isEmpty(),
                    peer?.bluetoothAddress != null
                )
            }
    }

    companion object {
        const val ARG_PUBLIC_KEY = "public_key"
        const val ARG_NAME = "name"
        private const val MAX_CHATS = 3
    }

}

