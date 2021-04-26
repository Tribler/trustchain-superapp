package nl.tudelft.trustchain.peerchat.ui.contacts

import android.bluetooth.BluetoothManager
import android.content.res.ColorStateList
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getColor
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.fragment_contacts.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.peerchat.R
import nl.tudelft.trustchain.peerchat.community.PeerChatCommunity
import nl.tudelft.trustchain.peerchat.databinding.FragmentContactsBinding
import nl.tudelft.trustchain.peerchat.db.PeerChatStore
import nl.tudelft.trustchain.peerchat.entity.ChatMessage
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
        return getIpv8().getOverlay()
            ?: throw java.lang.IllegalStateException("PeerChatCommunity is not configured")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter.registerRenderer(
            ContactItemRenderer(
                {
                    val args = Bundle()
                    args.putString(ConversationFragment.ARG_PUBLIC_KEY, it.publicKey.keyToBin().toHex())
                    args.putString(ConversationFragment.ARG_NAME, it.name)
                    findNavController().navigate(R.id.action_contactsFragment_to_conversationFragment, args)
                },
                {
                    showOptions(it)
                }
            )
        )

        lifecycleScope.launchWhenResumed {
            while (isActive) {
                // Refresh peer status periodically
                peers.value = getPeerChatCommunity().getPeers()
                updateConnectivityStatus()
                delay(1000L)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayout.VERTICAL
            )
        )

        binding.btnAddNearby.setOnClickListener {
            findNavController().navigate(R.id.action_contactsFragment_to_addNearbyFragment)
            fab.collapse()
        }

        binding.btnAddRemote.setOnClickListener {
            findNavController().navigate(R.id.action_contactsFragment_to_addRemoteFragment)
            fab.collapse()
        }

        items.observe(
            viewLifecycleOwner,
            Observer {
                adapter.updateItems(it)
                binding.imgEmpty.isVisible = it.isEmpty()
            }
        )
    }

    private fun createItems(
        contacts: List<Pair<Contact, ChatMessage?>>,
        peers: List<Peer>
    ): List<Item> {
        return contacts.filter { it.first.publicKey != getIpv8().myPeer.publicKey }
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

    @Suppress("DEPRECATION")
    private fun getConnectivityStatus(): Pair<String, Boolean> {
        val cm = getSystemService(requireContext(), ConnectivityManager::class.java)
        val activeNetwork = cm!!.activeNetworkInfo
        return if (activeNetwork != null) { // connected to the internet
            val type = if (activeNetwork.subtypeName.isNotBlank())
                activeNetwork.subtypeName else activeNetwork.typeName
            Pair(type, activeNetwork.isConnected)
        } else {
            Pair("Internet", false)
        }
    }

    private fun getBluetoothStatus(): Boolean {
        val bluetoothManager = requireContext().getSystemService<BluetoothManager>()
            ?: throw IllegalStateException("BluetoothManager not found")
        val bluetoothAdapter = bluetoothManager.adapter
        return bluetoothAdapter?.isEnabled ?: false
    }

    private fun updateConnectivityStatus() {
        val (networkType, isConnected) = getConnectivityStatus()
        binding.btnInternet.text = when (networkType) {
            "WIFI" -> "Wi-Fi"
            else -> networkType
        }
        val green = getColor(requireContext(), R.color.green)
        val red = getColor(requireContext(), R.color.red)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val color = if (isConnected) green else red
            binding.btnInternet.compoundDrawableTintList = ColorStateList.valueOf(color)
        }
        val bluetoothStatus = getBluetoothStatus()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val color = if (bluetoothStatus) green else red
            binding.btnBluetooth.compoundDrawableTintList = ColorStateList.valueOf(color)
        }
    }

    private fun showOptions(contact: Contact) {
        val items = arrayOf("Rename", "Delete")
        AlertDialog.Builder(requireContext())
            .setItems(items) { _, which ->
                when (which) {
                    0 -> renameContact(contact)
                    1 -> deleteContact(contact)
                }
            }
            .show()
    }

    private fun renameContact(contact: Contact) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Rename Contact")

        // Set up the input
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(contact.name)
        builder.setView(input)

        // Set up the buttons
        builder.setPositiveButton(
            "Rename"
        ) { _, _ ->
            store.contactsStore.updateContact(contact.publicKey, input.text.toString())
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun deleteContact(contact: Contact) {
        store.contactsStore.deleteContact(contact)
    }
}
