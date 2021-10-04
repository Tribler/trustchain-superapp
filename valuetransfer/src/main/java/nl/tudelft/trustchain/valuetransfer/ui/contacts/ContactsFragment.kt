package nl.tudelft.trustchain.valuetransfer.ui.contacts

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.view.animation.AnimationUtils
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
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.peerchat.entity.ChatMessage
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
        combine(getContactStore().getContacts(), peers) { contacts, peers ->
            createContactItems(contacts, peers)
        }.asLiveData()
    }

    private val chatItems: LiveData<List<Item>> by lazy {
        combine(getPeerChatStore().getLastMessages(
            isRecent = true,
            isArchive = false,
            isBlocked = false
        ), peers, getPeerChatStore().getAllContactState()) { messages, peers, state ->
            createChatItems(messages, peers, state)
        }.asLiveData()
    }

    private val archivedChatItems: LiveData<List<Item>> by lazy {
        combine(
            getPeerChatStore().getLastMessages(
                isRecent = false,
                isArchive = true,
                isBlocked = false
            ), peers, getPeerChatStore().getAllContactState()
        ) { messages, peers, state ->
            createChatItems(messages, peers, state)
        }.asLiveData()
    }

    private val blockedChatItems: LiveData<List<Item>> by lazy {
        combine(getPeerChatStore().getLastMessages(
            isRecent = false,
            isArchive = false,
            isBlocked = true
        ), peers, getPeerChatStore().getAllContactState()) { messages, peers, state ->
            createChatItems(messages, peers, state)
        }.asLiveData()
    }

    private var archivedChatsShown = false
    private var blockedChatsShown = false
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

    override fun onResume() {
        super.onResume()

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

        onResume()

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
        }

        onFocusChange(binding.etSearchContact, requireContext())

        binding.rvContacts.adapter = contactsAdapter
        binding.rvContacts.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        binding.rvRecentChats.adapter = chatsAdapter
        binding.rvRecentChats.layoutManager = LinearLayoutManager(context)

        binding.rvArchivedChats.adapter = archivedChatsAdapter
        binding.rvArchivedChats.layoutManager = LinearLayoutManager(context)

        binding.rvBlockedChats.adapter = blockedChatsAdapter
        binding.rvBlockedChats.layoutManager = LinearLayoutManager(context)

//        observeArchivedChats(viewLifecycleOwner, archivedChatsAdapter, archivedChatItems)
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
                    binding.clRecentChats.viewExitToLeft(requireContext())
                    binding.clArchivedChats.viewEnterFromRight(requireContext())
                }
                ADAPTER_BLOCKED -> {
                    binding.clRecentChats.viewExitToLeft(requireContext())
                    binding.clBlockedChats.viewEnterFromRight(requireContext())
                }
            }
        } else {
            when (from) {
                ADAPTER_ARCHIVE -> {
                    binding.clArchivedChats.viewExitToRight(requireContext())
                    binding.clRecentChats.viewEnterFromLeft(requireContext())
                }
                ADAPTER_BLOCKED -> {
                    binding.clBlockedChats.viewExitToRight(requireContext())
                    binding.clRecentChats.viewEnterFromLeft(requireContext())
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
                    val slideUpAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)
                    binding.clSearchbar.startAnimation(slideUpAnimation)
                    binding.etSearchContact.text = null

                    Handler().postDelayed({
                        binding.clSearchbar.isVisible = false
                    }, 200)
                } else {
                    binding.clSearchbar.isVisible = true
                    val slideDownAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_down)
                    binding.clSearchbar.startAnimation(slideDownAnimation)

                    binding.etSearchContact.showKeyboard(requireContext())
                }
            }
            R.id.actionAddContact -> ContactAddDialog(
                getTrustChainCommunity().myPeer.publicKey,
                null,
                null
            ).show(parentFragmentManager, tag)
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
            Observer { list ->

                when (type) {
                    ADAPTER_RECENT -> binding.tvNoRecentChats.isVisible = list.isEmpty()
                    ADAPTER_ARCHIVE -> binding.tvNoArchivedChats.isVisible = list.isEmpty()
                    ADAPTER_BLOCKED -> binding.tvNoBlockedChats.isVisible = list.isEmpty()
                }

                adapter.updateItems(list)
            }
        )
    }

    private fun observeContacts(owner: LifecycleOwner, adapter: ItemAdapter) {
        contactItems.observe(
            owner,
            Observer {
                val list = it.filter { item ->
                    (item as ChatItem).contact.name.contains(searchFilter, ignoreCase = true) ||
                        item.contact.mid.contains(searchFilter, ignoreCase = true) ||
                        item.contact.publicKey.keyToBin().toHex().contains(searchFilter, ignoreCase = true)
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
        return contacts
            .filter {
                it.publicKey != getTrustChainCommunity().myPeer.publicKey
            }
            .sortedBy { contact ->
                contact.name
            }
            .mapIndexed { _, contact ->
                val peer = peers.find { it.mid == contact.mid }
                ChatItem(
                    contact,
                    null,
                    peer != null && !peer.address.isEmpty(),
                    peer?.bluetoothAddress != null,
                    null
                )
            }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun createChatItems(
        messages: List<ChatMessage>,
        peers: List<Peer>,
        state: List<ContactState>
    ): List<Item> {
        return messages
            .map { message ->
                val publicKey = if (message.outgoing) message.recipient else message.sender
                val peer = peers.find { it.publicKey == publicKey }
                val contact = getContactStore().getContactFromPublicKey(publicKey)
                val status = state.firstOrNull { it.publicKey == publicKey }

                ChatItem(
                    Contact(
                        contact?.name ?: resources.getString(R.string.text_unknown_contact),
                        publicKey
                    ),
                    message,
                    isOnline = peer != null && !peer.address.isEmpty(),
                    isBluetooth = peer?.bluetoothAddress != null,
                    status
                )
            }
    }

    companion object {
        const val ADAPTER_RECENT = "recent"
        const val ADAPTER_ARCHIVE = "archive"
        const val ADAPTER_BLOCKED = "blocked"
    }
}
