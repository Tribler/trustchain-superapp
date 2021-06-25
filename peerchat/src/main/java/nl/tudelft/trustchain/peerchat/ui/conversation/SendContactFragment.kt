package nl.tudelft.trustchain.peerchat.ui.conversation

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.fragment_send_contact.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import nl.tudelft.ipv8.Peer
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.peerchat.PeerChatFragment
import nl.tudelft.trustchain.peerchat.R
import nl.tudelft.trustchain.peerchat.db.PeerChatStore
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.peerchat.ui.contacts.ContactItem
import nl.tudelft.trustchain.peerchat.ui.contacts.ContactItemRenderer

class SendContactFragment : PeerChatFragment(R.layout.fragment_send_contact) {

    private val store by lazy {
        PeerChatStore.getInstance(requireContext())
    }

    private val peers = MutableStateFlow<List<Peer>>(listOf())

    private val publicKeyBin by lazy {
        requireArguments().getString(TransferFragment.ARG_PUBLIC_KEY)!!
    }

    private val publicKey by lazy {
        defaultCryptoProvider.keyFromPublicBin(publicKeyBin.hexToBytes())
    }

    private val name by lazy {
        requireArguments().getString(TransferFragment.ARG_NAME)!!
    }

    private val adapter = ItemAdapter()

    private val items: LiveData<List<Item>> by lazy {
        store.getContactsWithLastMessages().map { contacts ->
            contacts.filter { it.first.publicKey != getIpv8().myPeer.publicKey &&
                it.first.publicKey != publicKey}
                .sortedBy { it.first.name }
                .map { contactWithMessage ->
                    val (contact, message) = contactWithMessage
                    ContactItem(
                        contact,
                        message,
                        false,
                        false
                    )
                }
        }.asLiveData()
    }

    private var selectedContact: Contact? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter.registerRenderer(
            ContactItemRenderer(
                {
                    selectedContact = it
                    // Update selected view
                }, {}, true
            )
        )

        peers.value = getPeerChatCommunity().getPeers()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayout.VERTICAL
            )
        )

        btnSendContact.setOnClickListener {
            if (selectedContact != null) {
                getPeerChatCommunity().sendContact(selectedContact!!, publicKey)
                findNavController().popBackStack(R.id.conversationFragment, false)
            } else {
                Toast.makeText(requireContext(), "No contact selected", Toast.LENGTH_SHORT).show()
            }
        }

        items.observe(
            viewLifecycleOwner,
            Observer {
                adapter.updateItems(it)
                imgEmpty.isVisible = it.isEmpty()
            }
        )

    }

    val DUMMY_CONTACT = Contact("Sharif Dummy",
        defaultCryptoProvider.generateKey().pub())
}
