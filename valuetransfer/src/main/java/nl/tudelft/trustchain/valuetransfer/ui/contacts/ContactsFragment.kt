package nl.tudelft.trustchain.valuetransfer.ui.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
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
import nl.tudelft.trustchain.valuetransfer.util.closeKeyboard
import nl.tudelft.trustchain.valuetransfer.util.onFocusChange
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.peerchat.community.PeerChatCommunity
import nl.tudelft.trustchain.peerchat.db.PeerChatStore
import nl.tudelft.trustchain.peerchat.entity.ChatMessage
import nl.tudelft.trustchain.peerchat.ui.contacts.ContactItem
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentContactsVtBinding
import nl.tudelft.trustchain.valuetransfer.dialogs.ContactAddDialog
import nl.tudelft.trustchain.valuetransfer.dialogs.ExchangeTransferMoneyDialog

class ContactsFragment : BaseFragment(R.layout.fragment_contacts_vt) {

    private val binding by viewBinding(FragmentContactsVtBinding::bind)
    private lateinit var parentActivity: ValueTransferMainActivity
    private lateinit var peerChatCommunity: PeerChatCommunity
    private lateinit var peerChatStore: PeerChatStore
    private lateinit var contactStore: ContactStore

    private val chatsAdapter = ItemAdapter()
    private val hiddenChatsAdapter = ItemAdapter()
    private val contactsAdapter = ItemAdapter()

    private val peers = MutableStateFlow<List<Peer>>(listOf())

    private val hiddenChatItems: LiveData<List<Item>> by lazy {
        combine(peerChatStore.getAllMessages(), peers, contactStore.getContacts()) { messages, peers, contacts ->
            createHiddenChatItems(messages, contacts, peers)
        }.asLiveData()
    }

    private val chatItems: LiveData<List<Item>> by lazy {
        combine(peerChatStore.getContactsWithLastMessages(), peers) { contacts, peers ->
            createChatItems(contacts
                .filter {
                    it.second?.timestamp != null &&
                        contactStore.getContactFromPublicKey(it.first.publicKey) != null
                }, peers)
        }.asLiveData()
    }

    private val contactItems: LiveData<List<Item>> by lazy {
        combine(contactStore.getContacts(), peers) { contacts, peers ->
            createContactItems(contacts, peers)
        }.asLiveData()
    }

    private var hiddenChatsShown = false
    private var searchFilter = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_contacts_vt, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        parentActivity = requireActivity() as ValueTransferMainActivity
        peerChatCommunity = parentActivity.getCommunity(ValueTransferMainActivity.peerChatCommunityTag) as PeerChatCommunity
        peerChatStore = parentActivity.getStore(ValueTransferMainActivity.peerChatStoreTag) as PeerChatStore
        contactStore = parentActivity.getStore(ValueTransferMainActivity.contactStoreTag) as ContactStore

        hiddenChatsAdapter.registerRenderer(
            ChatItemRenderer(
                {
                    val args = Bundle()
                    args.putString(ValueTransferMainActivity.ARG_PUBLIC_KEY, it.publicKey.keyToBin().toHex())
                    args.putString(ValueTransferMainActivity.ARG_NAME, it.name)
                    args.putString(ValueTransferMainActivity.ARG_PARENT, ValueTransferMainActivity.contactsFragmentTag)

                    parentActivity.detailFragment(ValueTransferMainActivity.contactChatFragmentTag, args)
                }, {}, {}
            )
        )

        chatsAdapter.registerRenderer(
            ChatItemRenderer(
                {
                    val args = Bundle()
                    args.putString(ValueTransferMainActivity.ARG_PUBLIC_KEY, it.publicKey.keyToBin().toHex())
                    args.putString(ValueTransferMainActivity.ARG_NAME, it.name)
                    args.putString(ValueTransferMainActivity.ARG_PARENT, ValueTransferMainActivity.contactsFragmentTag)

                    parentActivity.detailFragment(ValueTransferMainActivity.contactChatFragmentTag, args)
                }, { contact ->
                    ExchangeTransferMoneyDialog(contact, null, false).show(parentFragmentManager, tag)
                }, { contact ->
                    ExchangeTransferMoneyDialog(contact, null, true).show(parentFragmentManager, tag)
                }
            )
        )

        contactsAdapter.registerRenderer(
            ContactsItemRenderer {
                val args = Bundle()
                args.putString(ValueTransferMainActivity.ARG_PUBLIC_KEY, it.publicKey.keyToBin().toHex())
                args.putString(ValueTransferMainActivity.ARG_NAME, it.name)
                args.putString(ValueTransferMainActivity.ARG_PARENT, ValueTransferMainActivity.contactsFragmentTag)

                parentActivity.detailFragment(ValueTransferMainActivity.contactChatFragmentTag, args)
            }
        )

        lifecycleScope.launchWhenResumed {
            while (isActive) {
                peers.value = peerChatCommunity.getPeers()
                delay(1000L)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        parentActivity.setActionBarTitle("Contacts")
        parentActivity.toggleActionBar(false)
        parentActivity.toggleBottomNavigation(true)

//        lifecycleScope.launchWhenStarted {
//            while(isActive) {
//                hiddenChatItems.value = hiddenChatItems.value
//            }
//        }

//        hiddenChatsAdapter.updateItems(emptyList())
//        hiddenChatsAdapter.notifyDataSetChanged()
//        rvHiddenChats.recycledViewPool.clear()

//        observeHiddenChats(this, hiddenChatsAdapter, hiddenChatItems)
//        observeChats(this, chatsAdapter, chatItems)
//        observeContacts(this, contactsAdapter)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onResume()

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

        onFocusChange(binding.etSearchContact, requireContext())

        binding.ivAddContactButton.setOnClickListener {
            ContactAddDialog(getTrustChainCommunity().myPeer.publicKey, null, null).show(parentFragmentManager, tag)
        }

        binding.rvHiddenChats.adapter = hiddenChatsAdapter
        binding.rvHiddenChats.layoutManager = LinearLayoutManager(context)

        binding.rvChats.adapter = chatsAdapter
        binding.rvChats.layoutManager = LinearLayoutManager(context)

        binding.rvContacts.adapter = contactsAdapter
        binding.rvContacts.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        observeHiddenChats(viewLifecycleOwner, hiddenChatsAdapter, hiddenChatItems)
        observeChats(viewLifecycleOwner, chatsAdapter, chatItems)
        observeContacts(viewLifecycleOwner, contactsAdapter)

        binding.clTogglePendingChats.setOnClickListener {
            hiddenChatsShown = !hiddenChatsShown

            binding.clShowHiddenChats.isVisible = !hiddenChatsShown
            binding.clHideHiddenChats.isVisible = hiddenChatsShown
            binding.rvHiddenChats.isVisible = hiddenChatsShown

            binding.tvNoChats.isVisible = chatsAdapter.itemCount == 0 && hiddenChatsShown == false
        }
    }

    private fun observeHiddenChats(owner: LifecycleOwner, adapter: ItemAdapter, items: LiveData<List<Item>>) {
        items.observe(
            owner,
            Observer { list ->
                if(list.isEmpty()) {
                    binding.rvHiddenChats.isVisible = false
                    hiddenChatsShown = false
                }

                binding.clTogglePendingChats.isVisible = list.isNotEmpty()
                adapter.updateItems(list)
            }
        )
    }

    private fun observeChats(owner: LifecycleOwner, adapter: ItemAdapter, items: LiveData<List<Item>>) {
        items.observe(
            owner,
            Observer { list ->
                binding.tvNoChats.isVisible = list.isEmpty() && hiddenChatsShown == false
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

                binding.tvNoContacts.isVisible = list.isEmpty()
            }
        )
    }

    private fun createContactItems(
        contacts: List<Contact>,
        peers: List<Peer>
    ): List<Item> {
        return contacts.filter {
                it.publicKey != getTrustChainCommunity().myPeer.publicKey
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

    private fun createHiddenChatItems(
        messages: List<ChatMessage>,
        contacts: List<Contact>,
        peers: List<Peer>
    ): List<Item> {
        return messages
            .filter { message ->
                contacts.none { contact ->
                    contact.publicKey == if(message.outgoing) message.recipient else message.sender
                }
            }
            .sortedByDescending {
                it.timestamp.time
            }
            .distinctBy {
                if(it.outgoing) it.recipient else it.sender
            }
            .map { message ->
                val publicKey = if(message.outgoing) message.recipient else message.sender
                val peer = peers.find { it.publicKey == publicKey }

                ContactItem(
                    Contact("Unknown contact", publicKey),
                    message,
                    isOnline = peer != null && !peer.address.isEmpty(),
                    isBluetooth = peer?.bluetoothAddress != null
                )
            }
    }

    private fun createChatItems(
        contacts: List<Pair<Contact, ChatMessage?>>,
        peers: List<Peer>
    ): List<Item> {
        return contacts.filter {
                it.first.publicKey != getTrustChainCommunity().myPeer.publicKey
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
}
