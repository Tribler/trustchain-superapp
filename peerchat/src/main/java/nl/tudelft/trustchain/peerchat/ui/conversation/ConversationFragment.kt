package nl.tudelft.trustchain.peerchat.ui.conversation

import android.os.Bundle
import android.view.View
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.fragment_add_contact.*
import kotlinx.android.synthetic.main.fragment_conversation.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.peerchat.R
import nl.tudelft.trustchain.peerchat.community.PeerChatCommunity
import nl.tudelft.trustchain.peerchat.databinding.FragmentConversationBinding
import nl.tudelft.trustchain.peerchat.db.PeerChatStore
import nl.tudelft.trustchain.peerchat.entity.ChatMessage

class ConversationFragment : BaseFragment(R.layout.fragment_conversation) {
    private val binding by viewBinding(FragmentConversationBinding::bind)

    private val adapter = ItemAdapter()

    private val items: LiveData<List<Item>> by lazy {
        store.getAllByPublicKey(publicKey).map { messages ->
            createItems(messages)
        }.asLiveData()
    }

    private val store by lazy {
        PeerChatStore.getInstance(requireContext())
    }

    private val publicKey by lazy {
        val publicKeyBin = requireArguments().getString(ARG_PUBLIC_KEY)!!
        defaultCryptoProvider.keyFromPublicBin(publicKeyBin.hexToBytes())
    }

    private val name by lazy {
        requireArguments().getString(ARG_NAME)!!
    }

    private fun getPeerChatCommunity(): PeerChatCommunity {
        return getIpv8().getOverlay() ?: throw java.lang.IllegalStateException("PeerChatCommunity is not configured")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter.registerRenderer(ChatMessageItemRenderer())

        items.observe(this, Observer {
            val oldCount = adapter.itemCount
            adapter.updateItems(it)
            if (adapter.itemCount != oldCount) {
                // New message, scroll to the bottom
                binding.recyclerView.scrollToPosition(adapter.itemCount - 1)
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)

        btnSend.setOnClickListener {
            val message = binding.edtMessage.text.toString()
            if (message.isNotEmpty()) {
                getPeerChatCommunity().sendMessage(message, publicKey)
                binding.edtMessage.text = null
            }
        }
    }

    private fun createItems(messages: List<ChatMessage>): List<Item> {
        return messages.mapIndexed { index, chatMessage ->
            val shouldShowAvatar = !chatMessage.outgoing && (
                index == messages.size - 1 ||
                messages[index + 1].outgoing != chatMessage.outgoing)
            /*
            val shouldShowDate = (index == messages.size - 1 ||
                messages[index + 1].outgoing != chatMessage.outgoing ||
                (messages[index + 1].timestamp.time - chatMessage.timestamp.time > GROUP_TIME_LIMIT))
             */
            val shouldShowDate = true
            ChatMessageItem(chatMessage, shouldShowAvatar, shouldShowDate, name)
        }
    }

    companion object {
        const val ARG_PUBLIC_KEY = "public_key"
        const val ARG_NAME = "name"

        private const val GROUP_TIME_LIMIT = 60 * 1000
    }
}
