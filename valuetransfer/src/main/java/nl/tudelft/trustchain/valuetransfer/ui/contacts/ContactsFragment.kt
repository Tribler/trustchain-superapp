package nl.tudelft.trustchain.valuetransfer.ui.contacts

import android.os.Bundle
import android.view.*
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.common.valuetransfer.extensions.exitEnterView
import nl.tudelft.trustchain.peerchat.entity.ChatMessage
import nl.tudelft.trustchain.peerchat.entity.ContactImage
import nl.tudelft.trustchain.peerchat.entity.ContactState
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ui.VTFragment
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentContactsVtBinding
import nl.tudelft.trustchain.valuetransfer.dialogs.ContactAddDialog
import nl.tudelft.trustchain.valuetransfer.dialogs.OptionsDialog
import nl.tudelft.trustchain.valuetransfer.util.*

class ContactsFragment : VTFragment(R.layout.fragment_contacts_vt) {
    private val binding by viewBinding(FragmentContactsVtBinding::bind)

    private val chatsAdapter = ItemAdapter()
    private val archivedChatsAdapter = ItemAdapter()
    private val blockedChatsAdapter = ItemAdapter()
    private val contactsAdapter = ItemAdapter()

    private val peers = MutableStateFlow<List<Peer>>(listOf())

    private val contactItems: LiveData<List<Item>> by lazy {
        combine(
            getContactStore().getContacts(),
            peers,
            getPeerChatStore().getAllContactImages()
        ) { contacts, peers, images ->
            createContactItems(contacts, peers, images)
        }.asLiveData()
    }

    private val chatItems: LiveData<List<Item>> by lazy {
        combine(
            getPeerChatStore().getLastMessages(
                isRecent = true,
                isArchive = false,
                isBlocked = false
            ),
            peers,
            getPeerChatStore().getAllContactState(),
            getPeerChatStore().getAllContactImages()
        ) { messages, peers, state, images ->
            createChatItems(messages, peers, state, images)
        }.asLiveData()
    }

    private val archivedChatItems: LiveData<List<Item>> by lazy {
        combine(
            getPeerChatStore().getLastMessages(
                isRecent = false,
                isArchive = true,
                isBlocked = false
            ),
            peers,
            getPeerChatStore().getAllContactState(),
            getPeerChatStore().getAllContactImages()
        ) { messages, peers, state, images ->
            createChatItems(messages, peers, state, images)
        }.asLiveData()
    }

    private val blockedChatItems: LiveData<List<Item>> by lazy {
        combine(
            getPeerChatStore().getLastMessages(
                isRecent = false,
                isArchive = false,
                isBlocked = true
            ),
            peers,
            getPeerChatStore().getAllContactState(),
            getPeerChatStore().getAllContactImages()
        ) { messages, peers, state, images ->
            createChatItems(messages, peers, state, images)
        }.asLiveData()
    }

    private var searchFilter = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_contacts_vt, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        contactsAdapter.registerRenderer(
            ContactsItemRenderer {
                val args = Bundle().apply {
                    putString(ValueTransferMainActivity.ARG_PUBLIC_KEY, it.publicKey.keyToBin().toHex())
                    putString(ValueTransferMainActivity.ARG_NAME, it.name)
                    putString(ValueTransferMainActivity.ARG_PARENT, ValueTransferMainActivity.contactsFragmentTag)
                }

                parentActivity.detailFragment(ValueTransferMainActivity.contactChatFragmentTag, args)
            }
        )

        chatsAdapter.registerRenderer(
            ChatItemRenderer {
                val args = Bundle().apply {
                    putString(ValueTransferMainActivity.ARG_PUBLIC_KEY, it.publicKey.keyToBin().toHex())
                    putString(ValueTransferMainActivity.ARG_NAME, it.name)
                    putString(ValueTransferMainActivity.ARG_PARENT, ValueTransferMainActivity.contactsFragmentTag)
                }

                parentActivity.detailFragment(ValueTransferMainActivity.contactChatFragmentTag, args)
            }
        )

        archivedChatsAdapter.registerRenderer(
            ChatItemRenderer {
                val args = Bundle().apply {
                    putString(ValueTransferMainActivity.ARG_PUBLIC_KEY, it.publicKey.keyToBin().toHex())
                    putString(ValueTransferMainActivity.ARG_NAME, it.name)
                    putString(ValueTransferMainActivity.ARG_PARENT, ValueTransferMainActivity.contactsFragmentTag)
                }

                parentActivity.detailFragment(ValueTransferMainActivity.contactChatFragmentTag, args)
            }
        )

        blockedChatsAdapter.registerRenderer(
            ChatItemRenderer {
                val args = Bundle().apply {
                    putString(ValueTransferMainActivity.ARG_PUBLIC_KEY, it.publicKey.keyToBin().toHex())
                    putString(ValueTransferMainActivity.ARG_NAME, it.name)
                    putString(ValueTransferMainActivity.ARG_PARENT, ValueTransferMainActivity.contactsFragmentTag)
                }

                parentActivity.detailFragment(ValueTransferMainActivity.contactChatFragmentTag, args)
            }
        )

        lifecycleScope.launchWhenResumed {
            while (isActive) {
                peers.value = getPeerChatCommunity().getPeers()
                delay(1000L)
            }
        }
    }

    override fun initView() {
        parentActivity.apply {
            setActionBarTitle(
                resources.getString(R.string.menu_navigation_contacts),
                null
            )
            toggleActionBar(false)
            toggleBottomNavigation(true)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()

        binding.ivSearchBarCancelIcon.setOnClickListener {
            etSearchContact.text = null
            etSearchContact.clearFocus()
            ivSearchBarCancelIcon.isVisible = false
            etSearchContact.closeKeyboard(requireContext())
        }

        binding.etSearchContact.doAfterTextChanged { searchText ->
            ivSearchBarCancelIcon.isVisible = searchText != null && searchText.isNotEmpty()
            searchFilter = searchText.toString()
            observeContacts(viewLifecycleOwner, contactsAdapter)
            observeChats(viewLifecycleOwner, chatsAdapter, chatItems, ADAPTER_RECENT)
            observeChats(viewLifecycleOwner, archivedChatsAdapter, archivedChatItems, ADAPTER_ARCHIVE)
            observeChats(viewLifecycleOwner, blockedChatsAdapter, blockedChatItems, ADAPTER_BLOCKED)
        }

        onFocusChange(binding.etSearchContact, requireContext())

        binding.rvContacts.apply {
            adapter = contactsAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        val drawable = ResourcesCompat.getDrawable(resources, R.drawable.divider_chat, requireContext().theme)

        binding.rvRecentChats.apply {
            adapter = chatsAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecorator(drawable!!) as RecyclerView.ItemDecoration)
        }

        binding.rvArchivedChats.apply {
            adapter = archivedChatsAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecorator(drawable!!) as RecyclerView.ItemDecoration)
        }

        binding.rvBlockedChats.apply {
            adapter = blockedChatsAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecorator(drawable!!) as RecyclerView.ItemDecoration)
        }

        binding.clAddNewContact.setOnClickListener {
            ContactAddDialog(
                getTrustChainCommunity().myPeer.publicKey,
                null,
                null
            ).show(parentFragmentManager, tag)
        }

        observeContacts(viewLifecycleOwner, contactsAdapter)
        observeChats(viewLifecycleOwner, chatsAdapter, chatItems, ADAPTER_RECENT)
        observeChats(viewLifecycleOwner, archivedChatsAdapter, archivedChatItems, ADAPTER_ARCHIVE)
        observeChats(viewLifecycleOwner, blockedChatsAdapter, blockedChatItems, ADAPTER_BLOCKED)

        binding.clArchivedBackToRecent.setOnClickListener {
            toggleChats(ADAPTER_ARCHIVE, ADAPTER_RECENT)
        }

        binding.clBlockedBackToRecent.setOnClickListener {
            toggleChats(ADAPTER_BLOCKED, ADAPTER_RECENT)
        }

        binding.clRecentChatsOptions.setOnClickListener {
            OptionsDialog(
                R.menu.chats_types,
                resources.getString(R.string.dialog_contacts_chats_view),
                optionSelected = { _, item ->
                    when (item.itemId) {
                        R.id.actionArchiveChats -> toggleChats(ADAPTER_RECENT, ADAPTER_ARCHIVE)
                        R.id.actionBlockedChats -> toggleChats(ADAPTER_RECENT, ADAPTER_BLOCKED)
                    }
                }
            ).show(parentFragmentManager, tag)
        }
    }

    private fun toggleChats(from: String, to: String) {
        if (from == ADAPTER_RECENT) {
            when (to) {
                ADAPTER_ARCHIVE -> {
                    binding.clRecentChats.exitEnterView(requireContext(), binding.clArchivedChats)
                }
                ADAPTER_BLOCKED -> {
                    binding.clRecentChats.exitEnterView(requireContext(), binding.clBlockedChats)
                }
            }
        } else {
            when (from) {
                ADAPTER_ARCHIVE -> {
                    binding.clArchivedChats.exitEnterView(requireContext(), binding.clRecentChats, false)
                }
                ADAPTER_BLOCKED -> {
                    binding.clBlockedChats.exitEnterView(requireContext(), binding.clRecentChats, false)
                }
            }
        }

        binding.tvNoRecentChats.isVisible = to == ADAPTER_RECENT && chatsAdapter.itemCount == 0
        binding.tvNoArchivedChats.isVisible = to == ADAPTER_ARCHIVE && archivedChatsAdapter.itemCount == 0
        binding.tvNoBlockedChats.isVisible = to == ADAPTER_BLOCKED && blockedChatsAdapter.itemCount == 0
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()

        inflater.inflate(R.menu.contacts_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.actionSearch -> {
                if (binding.clSearchbar.isVisible) {
                    binding.etSearchContact.text = null
                    binding.clSearchbar.isVisible = false
                } else {
                    binding.clSearchbar.isVisible = true
                    binding.etSearchContact.showKeyboard(requireContext())
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun observeChats(
        owner: LifecycleOwner,
        adapter: ItemAdapter,
        items: LiveData<List<Item>>,
        type: String
    ) {
        items.observe(
            owner,
            Observer {
                it.filter { item ->
                    (item as ChatItem).contact.name.contains(searchFilter, ignoreCase = true) ||
                        item.contact.mid.contains(searchFilter, ignoreCase = true) ||
                        item.contact.publicKey.keyToBin().toHex().contains(searchFilter, ignoreCase = true)
                }.let { list ->
                    adapter.updateItems(list)

                    when (type) {
                        ADAPTER_RECENT -> binding.tvNoRecentChats.isVisible = list.isEmpty()
                        ADAPTER_ARCHIVE -> binding.tvNoArchivedChats.isVisible = list.isEmpty()
                        ADAPTER_BLOCKED -> binding.tvNoBlockedChats.isVisible = list.isEmpty()
                    }
                }
            }
        )
    }

    private fun observeContacts(owner: LifecycleOwner, adapter: ItemAdapter) {
        contactItems.observe(
            owner,
            Observer {
                it.filter { item ->
                    (item as ChatItem).contact.name.contains(searchFilter, ignoreCase = true) ||
                        item.contact.mid.contains(searchFilter, ignoreCase = true) ||
                        item.contact.publicKey.keyToBin().toHex().contains(searchFilter, ignoreCase = true)
                }.let { list ->
                    adapter.updateItems(list)

                    binding.tvNoContacts.isVisible = list.isEmpty()
                }
            }
        )
    }

    private fun createContactItems(
        contacts: List<Contact>,
        peers: List<Peer>,
        images: List<ContactImage>
    ): List<Item> {
        return contacts
            .filter {
                it.publicKey != getTrustChainCommunity().myPeer.publicKey
            }
            .sortedBy { contact ->
                contact.name
            }
            .mapIndexed { _, contact ->
                val peer = peers.find { it.mid == contact.mid }
                val image = images.firstOrNull { it.publicKey == contact.publicKey }
                ChatItem(
                    contact,
                    null,
                    peer != null && !peer.address.isEmpty(),
                    peer?.bluetoothAddress != null,
                    null,
                    image
                )
            }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun createChatItems(
        messages: List<ChatMessage>,
        peers: List<Peer>,
        state: List<ContactState>,
        images: List<ContactImage>
    ): List<Item> {
        return messages
            .map { message ->
                val publicKey = if (message.outgoing) message.recipient else message.sender
                val peer = peers.find { it.publicKey == publicKey }
                val contact = getContactStore().getContactFromPublicKey(publicKey)
                val status = state.firstOrNull { it.publicKey == publicKey }
                val image = images.firstOrNull { it.publicKey == publicKey }
                val identityName = status?.identityInfo?.let {
                    "${it.initials} ${it.surname}"
                }

                ChatItem(
                    Contact(
                        contact?.name ?: (identityName ?: resources.getString(R.string.text_unknown_contact)),
                        publicKey
                    ),
                    message,
                    isOnline = peer != null && !peer.address.isEmpty(),
                    isBluetooth = peer?.bluetoothAddress != null,
                    status,
                    image
                )
            }
    }

    companion object {
        const val ADAPTER_RECENT = "recent"
        const val ADAPTER_ARCHIVE = "archive"
        const val ADAPTER_BLOCKED = "blocked"
    }
}
