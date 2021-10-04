package nl.tudelft.trustchain.valuetransfer.ui.walletoverview

import android.content.Intent
import android.os.Bundle
import android.view.*
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
import nl.tudelft.trustchain.common.util.QRCodeUtils
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.peerchat.entity.ChatMessage
import nl.tudelft.trustchain.peerchat.entity.ContactState
import nl.tudelft.trustchain.peerchat.ui.contacts.ContactItem
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ui.VTFragment
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentWalletVtBinding
import nl.tudelft.trustchain.valuetransfer.dialogs.IdentityDetailsDialog
import nl.tudelft.trustchain.valuetransfer.dialogs.ExchangeTransferMoneyDialog
import nl.tudelft.trustchain.valuetransfer.dialogs.OptionsDialog
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import nl.tudelft.trustchain.valuetransfer.ui.QRScanController
import nl.tudelft.trustchain.valuetransfer.ui.contacts.ChatItem
import nl.tudelft.trustchain.valuetransfer.ui.contacts.ChatItemRenderer
import nl.tudelft.trustchain.valuetransfer.ui.identity.IdentityItem
import nl.tudelft.trustchain.valuetransfer.ui.identity.IdentityItemRenderer
import org.json.JSONObject

class WalletOverviewFragment : VTFragment(R.layout.fragment_wallet_vt) {
    private val binding by viewBinding(FragmentWalletVtBinding::bind)

    private val adapterIdentity = ItemAdapter()
    private val adapterContacts = ItemAdapter()

    private val itemsIdentity: LiveData<List<Item>> by lazy {
        getIdentityStore().getAllIdentities().map { identities ->
            createIdentityItems(identities)
        }.asLiveData()
    }

    private val peers = MutableStateFlow<List<Peer>>(listOf())

    private val itemsContacts: LiveData<List<Item>> by lazy {
        combine(getPeerChatStore().getLastMessages(
            isRecent = true,
            isArchive = false,
            isBlocked = false
        ), peers, getPeerChatStore().getAllContactState()) { messages, peers, state ->
            createChatItems(messages, peers, state)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

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
                val args = Bundle().apply {
                    putString(ValueTransferMainActivity.ARG_PUBLIC_KEY, it.publicKey.keyToBin().toHex())
                    putString(ValueTransferMainActivity.ARG_NAME, it.name)
                    putString(ValueTransferMainActivity.ARG_PARENT, ValueTransferMainActivity.walletOverviewFragmentTag)
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
                resources.getString(R.string.menu_navigation_wallet_overview),
                null
            )
            toggleActionBar(true)
            toggleBottomNavigation(getIdentityStore().hasIdentity())
        }

        binding.clNoIdentity.isVisible = !getIdentityStore().hasIdentity()
        binding.svHasIdentity.isVisible = getIdentityStore().hasIdentity()

        observeContactsItems(viewLifecycleOwner, adapterContacts, itemsContacts)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onResume()

        // IDENTITY
        binding.rvIdentities.adapter = adapterIdentity
        binding.rvIdentities.layoutManager = LinearLayoutManager(context)

        binding.btnCreateIdentity.setOnClickListener {
//            IdentityDetailsDialog().show(parentFragmentManager, tag)

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

        binding.clExchangeOptions.setOnClickListener {
            OptionsDialog(R.menu.exchange_options, "Choose Option") { _, item ->
                when (item.itemId) {
                    R.id.actionDeposit -> {
                        scanIntent = DEPOSIT_INTENT
                        QRCodeUtils(requireContext()).startQRScanner(
                            this,
                            promptText = resources.getString(R.string.text_scan_qr_exchange_buy),
                            vertical = true
                        )
                    }
                    R.id.actionWithdraw -> {
                        scanIntent = WITHDRAW_INTENT
                        QRCodeUtils(requireContext()).startQRScanner(
                            this,
                            promptText = resources.getString(R.string.text_scan_qr_exchange_sell),
                            vertical = true
                        )
                    }
                    R.id.actionTransferByQR -> {
                        scanIntent = TRANSFER_INTENT
                        QRCodeUtils(requireContext()).startQRScanner(
                            this,
                            promptText = resources.getString(R.string.text_scan_qr_exchange_transfer),
                            vertical = true
                        )
                    }
                    R.id.actionTransferToContact -> ExchangeTransferMoneyDialog(
                        null,
                        null,
                        true
                    ).show(parentFragmentManager, tag)
                    R.id.actionRequestTransferContact -> ExchangeTransferMoneyDialog(
                        null,
                        null,
                        false
                    ).show(parentFragmentManager, tag)
                }
            }.show(parentFragmentManager, tag)
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

        binding.btnShowMoreChats.setOnClickListener {
            parentActivity.selectBottomNavigationItem(ValueTransferMainActivity.contactsFragmentTag)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.wallet_overview_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.actionSettings -> {
                parentActivity.detailFragment(ValueTransferMainActivity.settingsFragmentTag, Bundle())
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        QRCodeUtils(requireContext()).parseActivityResult(requestCode, resultCode, data)?.let { result ->
            val obj = JSONObject(result)

            when (scanIntent) {
                DEPOSIT_INTENT -> {
                    if (obj.has(QRScanController.KEY_AMOUNT)) {
                        parentActivity.displaySnackbar(
                            requireContext(),
                            resources.getString(R.string.snackbar_exchange_scan_buy_not_sell),
                            type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR,
                            isShort = false
                        )
                        return
                    }
                    getQRScanController().exchangeMoney(obj, true)
                }
                WITHDRAW_INTENT -> {
                    if (!obj.has(QRScanController.KEY_AMOUNT)) {
                        parentActivity.displaySnackbar(
                            requireContext(),
                            resources.getString(R.string.snackbar_exchange_scan_sell_not_buy),
                            type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR,
                            isShort = false
                        )
                        return
                    }
                    getQRScanController().exchangeMoney(obj, false)
                }
                TRANSFER_INTENT -> {
                    if (obj.has(QRScanController.KEY_PAYMENT_ID)) {
                        parentActivity.displaySnackbar(
                            requireContext(),
                            resources.getString(R.string.snackbar_exchange_scan_transfer_not_buy_sell),
                            type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR,
                            isShort = false
                        )
                        return
                    }
                    getQRScanController().transferMoney(obj)
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

    private fun createChatItems(
        messages: List<ChatMessage>,
        peers: List<Peer>,
        state: List<ContactState>
    ): List<Item> {
        return messages
            .filterIndexed { index, _ ->
                binding.btnShowMoreChats.isVisible = index >= MAX_CHATS
                index < MAX_CHATS
            }
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
        private const val MAX_CHATS = 3
        private const val TRANSFER_INTENT = 0
        private const val DEPOSIT_INTENT = 1
        private const val WITHDRAW_INTENT = 2
    }
}
