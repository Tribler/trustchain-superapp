package nl.tudelft.trustchain.valuetransfer.ui.walletoverview

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.peerchat.community.PeerChatCommunity
import nl.tudelft.trustchain.peerchat.db.PeerChatStore
import nl.tudelft.trustchain.peerchat.entity.ChatMessage
import nl.tudelft.trustchain.peerchat.ui.contacts.ContactItem
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.community.IdentityCommunity
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentWalletVtBinding
import nl.tudelft.trustchain.valuetransfer.db.IdentityStore
import nl.tudelft.trustchain.valuetransfer.dialogs.IdentityDetailsDialog
import nl.tudelft.trustchain.valuetransfer.dialogs.ExchangeTransferMoneyDialog
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import nl.tudelft.trustchain.valuetransfer.ui.contacts.ChatItemRenderer
import nl.tudelft.trustchain.valuetransfer.ui.identity.IdentityItem
import nl.tudelft.trustchain.valuetransfer.ui.identity.IdentityItemRenderer
import org.json.JSONObject

class WalletOverviewFragment : BaseFragment(R.layout.fragment_wallet_vt) {

    private val binding by viewBinding(FragmentWalletVtBinding::bind)
    private lateinit var parentActivity: ValueTransferMainActivity
    private lateinit var identityCommunity: IdentityCommunity
    private lateinit var peerChatCommunity: PeerChatCommunity
    private lateinit var identityStore: IdentityStore
    private lateinit var peerChatStore: PeerChatStore
    private lateinit var contactStore: ContactStore

    private val adapterIdentity = ItemAdapter()
    private val adapterContacts = ItemAdapter()

    private val itemsIdentity: LiveData<List<Item>> by lazy {
        identityStore.getAllIdentities().map { identities ->
            createIdentityItems(identities)
        }.asLiveData()
    }

    private val peers = MutableStateFlow<List<Peer>>(listOf())

    private val itemsContacts: LiveData<List<Item>> by lazy {
        combine(peerChatStore.getContactsWithLastMessages(), peers) { contacts, peers ->
            createContactsItems(
                contacts.filter {
                    it.second?.timestamp != null && contactStore.getContactFromPublicKey(it.first.publicKey) != null
                },
                peers
            )
        }.asLiveData()
    }

    private var scanIntent: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wallet_vt, container, false)
    }

    override fun onResume() {
        super.onResume()

        parentActivity.setActionBarTitle("Wallet")
        parentActivity.toggleActionBar(true)
        parentActivity.toggleBottomNavigation(identityStore.hasIdentity())

        binding.clNoIdentity.isVisible = !identityStore.hasIdentity()
        binding.svHasIdentity.isVisible = identityStore.hasIdentity()

        observeContactsItems(viewLifecycleOwner, adapterContacts, itemsContacts)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        parentActivity = requireActivity() as ValueTransferMainActivity
        identityCommunity = parentActivity.getCommunity(ValueTransferMainActivity.identityCommunityTag) as IdentityCommunity
        peerChatCommunity = parentActivity.getCommunity(ValueTransferMainActivity.peerChatCommunityTag) as PeerChatCommunity
        identityStore = parentActivity.getStore(ValueTransferMainActivity.identityStoreTag) as IdentityStore
        peerChatStore = parentActivity.getStore(ValueTransferMainActivity.peerChatStoreTag) as PeerChatStore
        contactStore = parentActivity.getStore(ValueTransferMainActivity.contactStoreTag) as ContactStore

        // IDENTITY

        adapterIdentity.registerRenderer(
            IdentityItemRenderer(
                0,
                {
                    parentActivity.selectBottomNavigationItem(ValueTransferMainActivity.identityFragmentTag)
                },
                {}
            )
        )

        // CONTACTS

        adapterContacts.registerRenderer(
            ChatItemRenderer {
                val args = Bundle()
                args.putString(ValueTransferMainActivity.ARG_PUBLIC_KEY, it.publicKey.keyToBin().toHex())
                args.putString(ValueTransferMainActivity.ARG_NAME, it.name)
                args.putString(ValueTransferMainActivity.ARG_PARENT, ValueTransferMainActivity.walletOverviewFragmentTag)

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onResume()

        // IDENTITY
        binding.rvIdentities.adapter = adapterIdentity
        binding.rvIdentities.layoutManager = LinearLayoutManager(context)

        binding.btnCreateIdentity.setOnClickListener {
            IdentityDetailsDialog().show(parentFragmentManager, tag)
        }

        itemsIdentity.observe(
            viewLifecycleOwner,
            Observer {
                adapterIdentity.updateItems(it)
            }
        )

        binding.ivGoToIdentity.setOnClickListener {
            parentActivity.selectBottomNavigationItem(ValueTransferMainActivity.identityFragmentTag)
        }

        // EXCHANGE

        parentActivity.getBalance(true).observe(
            viewLifecycleOwner,
            Observer {
                if (it != binding.tvBalanceAmount.text.toString()) {
                    binding.tvBalanceAmount.text = it
                    binding.pbBalanceUpdating.isVisible = false
                }
            }
        )

        binding.clTransferQR.setOnClickListener {
            scanIntent = TRANSFER_INTENT
            QRCodeUtils(requireContext()).startQRScanner(this, promptText = "Scan QR Code to transfer EuroToken(s)", vertical = true)
        }

        binding.clTransferContact.setOnClickListener {
            ExchangeTransferMoneyDialog(null, null, true).show(parentFragmentManager, tag)
        }

        binding.clRequest.setOnClickListener {
            ExchangeTransferMoneyDialog(null, null, false).show(parentFragmentManager, tag)
        }

        binding.clButtonBuy.setOnClickListener {
            scanIntent = BUY_EXCHANGE_INTENT
            QRCodeUtils(requireContext()).startQRScanner(this, promptText = "Scan Buy EuroToken QR Code from Exchange", vertical = true)
        }

        binding.clButtonSell.setOnClickListener {
            scanIntent = SELL_EXCHANGE_INTENT
            QRCodeUtils(requireContext()).startQRScanner(this, promptText = "Scan Sell EuroToken QR Code from Exchange", vertical = true)
        }

        binding.ivGoToExchange.setOnClickListener {
            parentActivity.selectBottomNavigationItem(ValueTransferMainActivity.exchangeFragmentTag)
        }

        // CONTACTS
        binding.rvContactChats.adapter = adapterContacts
        binding.rvContactChats.layoutManager = LinearLayoutManager(context)

        observeContactsItems(viewLifecycleOwner, adapterContacts, itemsContacts)

        binding.ivGoToContacts.setOnClickListener {
            parentActivity.selectBottomNavigationItem(ValueTransferMainActivity.contactsFragmentTag)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        QRCodeUtils(requireContext()).parseActivityResult(requestCode, resultCode, data)?.let { result ->
            val obj = JSONObject(result)

            when (scanIntent) {
                TRANSFER_INTENT -> {
                    if (obj.has("payment_id")) {
                        parentActivity.displaySnackbar(requireContext(), "Please scan a transfer QR-code instead of buy or sell", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR, isShort = false)
                        return
                    }
                    parentActivity.getQRScanController().transferMoney(obj)
                }
                BUY_EXCHANGE_INTENT -> {
                    if (obj.has("amount")) {
                        parentActivity.displaySnackbar(requireContext(), "Please scan a buy QR-code instead of sell", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR, isShort = false)
                        return
                    }
                    parentActivity.getQRScanController().exchangeMoney(obj, true)
                }
                SELL_EXCHANGE_INTENT -> {
                    if (!obj.has("amount")) {
                        parentActivity.displaySnackbar(requireContext(), "Please scan a sell QR-code instead of buy", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR, isShort = false)
                        return
                    }
                    parentActivity.getQRScanController().exchangeMoney(obj, false)
                }
            }
        }
    }

    private fun observeContactsItems(
        owner: LifecycleOwner,
        adapter: ItemAdapter,
        items: LiveData<List<Item>>
    ) {
        items.observe(
            owner,
            Observer { list ->
                binding.tvNoChats.isVisible = list.isEmpty()
                adapter.updateItems(list)
                binding.rvContactChats.setItemViewCacheSize(list.size)
            }
        )
    }

    private fun createIdentityItems(identities: List<Identity>): List<Item> {
        return identities.mapIndexed { _, identity ->
            IdentityItem(
                identity
            )
        }
    }

    private fun createContactsItems(contacts: List<Pair<Contact, ChatMessage?>>, peers: List<Peer>): List<Item> {
        return contacts.filter { it.first.publicKey != getTrustChainCommunity().myPeer.publicKey }
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
        private const val MAX_CHATS = 3
        private const val TRANSFER_INTENT = 0
        private const val BUY_EXCHANGE_INTENT = 1
        private const val SELL_EXCHANGE_INTENT = 2
    }
}
