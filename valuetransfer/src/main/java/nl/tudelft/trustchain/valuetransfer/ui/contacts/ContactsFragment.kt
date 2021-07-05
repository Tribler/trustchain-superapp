package nl.tudelft.trustchain.valuetransfer.ui.contacts

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.*
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.fragment_contacts_vt.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.closeKeyboard
import nl.tudelft.trustchain.common.util.onFocusChange
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.peerchat.community.PeerChatCommunity
import nl.tudelft.trustchain.peerchat.db.PeerChatStore
import nl.tudelft.trustchain.peerchat.entity.ChatMessage
import nl.tudelft.trustchain.peerchat.ui.contacts.ContactItem
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentContactsVtBinding
import nl.tudelft.trustchain.valuetransfer.dialogs.ContactAddDialog

class ContactsFragment : BaseFragment(R.layout.fragment_contacts_vt) {

    private val binding by viewBinding(FragmentContactsVtBinding::bind)

    private val chatsAdapter = ItemAdapter()
    private val contactsAdapter = ItemAdapter()

    private val peerChatStore by lazy {
        PeerChatStore.getInstance(requireContext())
    }

    private val contactStore by lazy {
        ContactStore.getInstance(requireContext())
    }

    private val peers = MutableStateFlow<List<Peer>>(listOf())

    private val chatItems: LiveData<List<Item>> by lazy {
        combine(peerChatStore.getContactsWithLastMessages(), peers) { contacts, peers ->
            createChatItems(contacts, peers)
        }.asLiveData()
    }

    private val contactItems: LiveData<List<Item>> by lazy {
        combine(contactStore.getContacts(), peers) { contacts, peers ->
            createContactItems(contacts, peers)
        }.asLiveData()
    }
//
//    private val contactItems: LiveData<List<Item>> by lazy {
//        combine(peerChatStore.getContactsWithLastMessages(), peers) { contacts, peers ->
//            var moreContacts = contacts
//            moreContacts += contacts
//            moreContacts += contacts
//            moreContacts += contacts
//            createItems(moreContacts, peers)
//        }.asLiveData()
//    }

    private fun getPeerChatCommunity(): PeerChatCommunity {
        return getIpv8().getOverlay()
            ?: throw java.lang.IllegalStateException("PeerChatCommunity is not configured")
    }

    private var searchFilter = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        chatsAdapter.registerRenderer(
            ChatItemRenderer(
                {
                    val args = Bundle()
                    args.putString(ARG_PUBLIC_KEY, it.publicKey.keyToBin().toHex())
                    args.putString(ARG_NAME, it.name)
                    findNavController().navigate(nl.tudelft.trustchain.valuetransfer.R.id.action_chatsFragment_to_contactChatFragment, args)
                }, {
                   Log.d("TESTJE", "ON EXCHANGE CLICK")
                },
                0
            )
        )

        contactsAdapter.registerRenderer(
            ContactsItemRenderer {
                val args = Bundle()
                args.putString(ARG_PUBLIC_KEY, it.publicKey.keyToBin().toHex())
                args.putString(ARG_NAME, it.name)
                findNavController().navigate(nl.tudelft.trustchain.valuetransfer.R.id.action_chatsFragment_to_contactChatFragment, args)
            }
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

        val bottomNavigationView = requireActivity().findViewById<BottomNavigationView>(R.id.bnvChats)
        val navController = requireActivity().findNavController(R.id.navHostFragment)
        bottomNavigationView.setupWithNavController(navController)

        binding.ivSearchBarCancelIcon.setOnClickListener {
            etSearchContact.text = null
            etSearchContact.clearFocus()
            ivSearchBarCancelIcon.isVisible = false
            closeKeyboard(requireContext(), etSearchContact)
        }

        binding.etSearchContact.doAfterTextChanged { searchText ->
            ivSearchBarCancelIcon.isVisible = searchText != null && searchText.isNotEmpty()
            searchFilter = searchText.toString()
            observeContacts(viewLifecycleOwner, contactsAdapter)
        }

        binding.llContactNew.setOnClickListener {
            Log.d("TESTJE", "TESTJE")
            ContactAddDialog(getIpv8().myPeer.publicKey).show(parentFragmentManager, tag)
        }

        onFocusChange(binding.etSearchContact, requireContext())

        binding.rvChats.adapter = chatsAdapter
        binding.rvChats.layoutManager = LinearLayoutManager(context)
//        binding.rvChats.addItemDecoration(
//            DividerItemDecoration(
//                context,
//                LinearLayout.VERTICAL
//            )
//        )

        binding.rvContacts.adapter = contactsAdapter
        binding.rvContacts.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
//        PagerSnapHelper().attachToRecyclerView(binding.rvContacts)

        observeChats(viewLifecycleOwner, chatsAdapter)
        observeContacts(viewLifecycleOwner, contactsAdapter)
    }

    private fun observeChats(owner: LifecycleOwner, adapter: ItemAdapter) {
        chatItems.observe(
            owner,
            Observer { list ->
//                val list = it.sortedBy {
//                        item -> (item as ContactItem).lastMessage?.timestamp?.time
//                    Log.d("TESTJE", "${item.lastMessage?.timestamp?.time}")
//                }
                adapter.updateItems(list)
            }
        )
    }

    private fun observeContacts(owner: LifecycleOwner, adapter: ItemAdapter) {
        contactItems.observe(
            owner,
            Observer {
                val list = it.filter { item ->
                    (item as ContactItem).contact.name.contains(searchFilter, ignoreCase = true)
                        || item.contact.mid.contains(searchFilter, ignoreCase = true)
                        || item.contact.publicKey.keyToBin().toHex().contains(searchFilter, ignoreCase = true)
                }
                adapter.updateItems(list)
            }
        )
    }

    private fun createContactItems(
        contacts: List<Contact>,
        peers: List<Peer>
    ): List<Item> {
        return contacts.filter {
                it.publicKey != getIpv8().myPeer.publicKey
            }
            .sortedBy { contact ->
                contact.name
            }
            .mapIndexed { _, contact ->
                val peer = peers.find { it.mid == contact.mid }
                ContactItem(
                    contact,
                    null,
                    peer != null && !peer.address.isEmpty(),
                    peer?.bluetoothAddress != null
                )
            }
    }

    private fun createChatItems(
        contacts: List<Pair<Contact, ChatMessage?>>,
        peers: List<Peer>
    ): List<Item> {
        return contacts.filter {
                it.first.publicKey != getIpv8().myPeer.publicKey
            }
            .sortedByDescending { item ->
                item.second?.timestamp?.time
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
    }
}
