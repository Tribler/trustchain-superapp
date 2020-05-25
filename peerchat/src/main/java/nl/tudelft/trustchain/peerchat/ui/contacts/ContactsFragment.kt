package nl.tudelft.trustchain.peerchat.ui.contacts

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.lifecycle.*
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.fragment_contacts.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.peerchat.R
import nl.tudelft.trustchain.peerchat.community.PeerChatCommunity
import nl.tudelft.trustchain.peerchat.databinding.FragmentContactsBinding
import nl.tudelft.trustchain.peerchat.db.PeerChatStore
import nl.tudelft.trustchain.peerchat.entity.ChatMessage
import nl.tudelft.trustchain.peerchat.entity.Contact
import nl.tudelft.trustchain.peerchat.ui.conversation.ConversationFragment


@OptIn(ExperimentalCoroutinesApi::class)
class ContactsFragment : BaseFragment(R.layout.fragment_contacts) {
    private val binding by viewBinding(FragmentContactsBinding::bind)

    private val adapter = ItemAdapter()

    private val store by lazy {
        PeerChatStore.getInstance(requireContext())
    }

    private val peers = MutableStateFlow<List<Peer>>(listOf())

    private val items: LiveData<List<Item>> by lazy {
        combine(store.getContactsWithLastMessages(), peers) { contacts, peers ->
            createItems(contacts, peers)
        }.asLiveData()
    }

    private fun getPeerChatCommunity(): PeerChatCommunity {
        return getIpv8().getOverlay() ?: throw java.lang.IllegalStateException("PeerChatCommunity is not configured")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter.registerRenderer(ContactItemRenderer {
            val args = Bundle()
            args.putString(ConversationFragment.ARG_PUBLIC_KEY, it.publicKey.keyToBin().toHex())
            args.putString(ConversationFragment.ARG_NAME, it.name)
            findNavController().navigate(R.id.action_contactsFragment_to_conversationFragment, args)
        })

        lifecycleScope.launch {
            while (isActive) {
                // Refresh peer status periodically
                peers.value = getPeerChatCommunity().getPeers()
                delay(1000L)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.addItemDecoration(DividerItemDecoration(context, LinearLayout.VERTICAL))

        binding.btnAddNearby.setOnClickListener {
            // TODO
            fab.collapse()
        }

        binding.btnAddRemote.setOnClickListener {
            findNavController().navigate(R.id.action_contactsFragment_to_addRemoteFragment)
            fab.collapse()
        }

        items.observe(viewLifecycleOwner, Observer {
            adapter.updateItems(it)
            binding.imgEmpty.isVisible = it.isEmpty()
        })
    }

    private fun createItems(contacts: List<Pair<Contact, ChatMessage?>>, peers: List<Peer>): List<Item> {
        return contacts.map { contactWithMessage ->
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
}
