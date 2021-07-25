package nl.tudelft.trustchain.valuetransfer.ui.walletoverview

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
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
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
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
import nl.tudelft.trustchain.valuetransfer.dialogs.TransferMoneyDialog
import nl.tudelft.trustchain.valuetransfer.entity.Identity
import nl.tudelft.trustchain.valuetransfer.ui.contacts.ChatItemRenderer
import nl.tudelft.trustchain.valuetransfer.ui.identity.IdentityItem
import nl.tudelft.trustchain.valuetransfer.ui.identity.IdentityItemRenderer
import nl.tudelft.trustchain.valuetransfer.util.formatBalance

class WalletOverviewFragment : BaseFragment(R.layout.fragment_wallet_vt) {

    private val binding by viewBinding(FragmentWalletVtBinding::bind)

    private val identityStore by lazy {
        IdentityStore.getInstance(requireContext())
    }

    private val peerChatStore by lazy {
        PeerChatStore.getInstance(requireContext())
    }

    private val gatewayStore by lazy {
        GatewayStore.getInstance(requireContext())
    }

    private val transactionRepository by lazy {
        TransactionRepository(getIpv8().getOverlay()!!, gatewayStore)
    }

    private fun getPeerChatCommunity(): PeerChatCommunity {
        return getIpv8().getOverlay()
            ?: throw java.lang.IllegalStateException("PeerChatCommunity is not configured")
    }

    private fun getIdentityCommunity(): IdentityCommunity {
        return getIpv8().getOverlay()
            ?: throw java.lang.IllegalStateException("IdentityCommunity is not configured")
    }

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
            createContactsItems(contacts.filter {
                it.second?.timestamp != null &&
                    peerChatStore.contactsStore.getContactFromPublicKey(it.first.publicKey) != null
            }, peers)
        }.asLiveData()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wallet_vt, container, false)
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as ValueTransferMainActivity).setActionBarTitle("Wallet")
        (requireActivity() as ValueTransferMainActivity).toggleActionBar(true)
        (requireActivity() as ValueTransferMainActivity).toggleBottomNavigation(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getIdentityCommunity().createIdentitiesTable()
        getIdentityCommunity().createAttributesTable()

        // IDENTITY

        adapterIdentity.registerRenderer(
            IdentityItemRenderer(
                0, {
                    (requireActivity() as ValueTransferMainActivity).selectBottomNavigationItem(ValueTransferMainActivity.identityFragmentTag)
                }, {}
            )
        )

        // CONTACTS

        adapterContacts.registerRenderer(
            ChatItemRenderer(
                {
                    val args = Bundle()
                    args.putString(ValueTransferMainActivity.ARG_PUBLIC_KEY, it.publicKey.keyToBin().toHex())
                    args.putString(ValueTransferMainActivity.ARG_NAME, it.name)
                    args.putString(ValueTransferMainActivity.ARG_PARENT, ValueTransferMainActivity.walletOverviewFragmentTag)

//                    val contactChatFragment = ContactChatFragment()
//                    contactChatFragment.arguments = args
//                    (requireActivity() as ValueTransferMainActivity).pushFragment(contactChatFragment, ValueTransferMainActivity.contactChatFragmentTag)

                    (requireActivity() as ValueTransferMainActivity).detailFragment(ValueTransferMainActivity.contactChatFragmentTag, args)
                }, { contact ->
                    TransferMoneyDialog(contact, false, transactionRepository, getPeerChatCommunity()).show(parentFragmentManager, tag)
                }, { contact ->
                    TransferMoneyDialog(contact, true, transactionRepository, getPeerChatCommunity()).show(parentFragmentManager, tag)
                }
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

        onResume()

        view.rootView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimaryValueTransfer))

        // IDENTITY
        binding.tvIdentityTitle.isVisible = identityStore.hasPersonalIdentity()

        binding.rvIdentities.adapter = adapterIdentity
        binding.rvIdentities.layoutManager = LinearLayoutManager(context)

        binding.tvNoPersonalIdentity.setOnClickListener {
            IdentityDetailsDialog(null, getIdentityCommunity()).show(parentFragmentManager, tag)
        }

        itemsIdentity.observe(
            viewLifecycleOwner,
            Observer {
                binding.tvNoPersonalIdentityExplanation.isVisible = it.isEmpty()
                binding.tvNoPersonalIdentity.isVisible = it.isEmpty()
                adapterIdentity.updateItems(it)
            }
        )

        // EXCHANGE

        lifecycleScope.launchWhenCreated {
            binding.tvBalanceAmount.text = formatBalance(transactionRepository.getMyVerifiedBalance())
        }

        binding.clTransferQR.setOnClickListener {
            Log.d("TESTJE", "SEND QR CLICKED")

            QRCodeUtils(requireContext()).startQRScanner(this, vertical = true)

        }

        binding.clTransferContact.setOnClickListener {
            Log.d("TESTJE", "SEND INPUT CLICKED")
            TransferMoneyDialog(null, true, transactionRepository, getPeerChatCommunity()).show(parentFragmentManager, tag)
        }

        binding.clRequest.setOnClickListener {
            Log.d("TESTJE", "REQUEST CLICKED")
            TransferMoneyDialog(null, false, transactionRepository, getPeerChatCommunity()).show(parentFragmentManager, tag)
        }

        binding.clButtonBuy.setOnClickListener {
            Log.d("TESTJE", "CLICKED BUY TODO")
        }

        binding.clButtonSell.setOnClickListener {
            Log.d("TESTJE", "CLICKED SELL TODO")
        }

        // CONTACTS

        binding.rvContactChats.adapter = adapterContacts
        binding.rvContactChats.layoutManager = LinearLayoutManager(context)

        itemsContacts.observe(
            viewLifecycleOwner,
            Observer { list ->
                if(list.isEmpty()) {
                    binding.tvNoChats.visibility = View.VISIBLE
                }
                adapterContacts.updateItems(list)
            }
        )



//        binding.bnvWalletOverview.isVisible = identityStore.hasPersonalIdentity()
//        binding.fcvExchange.isVisible = identityStore.hasPersonalIdentity()
//        binding.fcvContacts.isVisible = identityStore.hasPersonalIdentity()

    }

    private fun createIdentityItems(identities: List<Identity>): List<Item> {
        return identities.mapIndexed { _, identity ->
            IdentityItem(
                identity
            )
        }
    }

    private fun createContactsItems(contacts: List<Pair<Contact, ChatMessage?>>, peers: List<Peer>): List<Item> {
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
        private const val MAX_CHATS = 3
    }
}
