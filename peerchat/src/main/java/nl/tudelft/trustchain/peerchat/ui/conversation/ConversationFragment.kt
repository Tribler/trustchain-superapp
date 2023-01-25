package nl.tudelft.trustchain.peerchat.ui.conversation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.fragment_conversation.*
import kotlinx.coroutines.flow.map
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.peerchat.R
import nl.tudelft.trustchain.peerchat.community.PeerChatCommunity
import nl.tudelft.trustchain.peerchat.databinding.FragmentConversationBinding
import nl.tudelft.trustchain.peerchat.db.PeerChatStore
import nl.tudelft.trustchain.peerchat.entity.ChatMessage
import nl.tudelft.trustchain.peerchat.util.saveFile

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

    private val gatewayStore by lazy {
        GatewayStore.getInstance(requireContext())
    }

    private val transactionRepository by lazy {
        TransactionRepository(getTrustChainCommunity(), gatewayStore)
    }

    private val publicKeyBin by lazy {
        requireArguments().getString(ARG_PUBLIC_KEY)!!
    }

    private val publicKey by lazy {
        defaultCryptoProvider.keyFromPublicBin(publicKeyBin.hexToBytes())
    }

    private val name by lazy {
        requireArguments().getString(ARG_NAME)!!
    }

    private val onCommitContentListener =
        InputConnectionCompat.OnCommitContentListener { inputContentInfo, flags, _ ->
            val lacksPermission = (
                flags and
                    InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
                ) != 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && lacksPermission) {
                try {
                    inputContentInfo.requestPermission()
                } catch (e: Exception) {
                    return@OnCommitContentListener false // return false if failed
                }
            }

            val uri = inputContentInfo.contentUri
            Log.d("ConversationFragment", "uri: $uri")

            sendImageFromUri(uri)

            true
        }

    private fun getPeerChatCommunity(): PeerChatCommunity {
        return getIpv8().getOverlay()
            ?: throw java.lang.IllegalStateException("PeerChatCommunity is not configured")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter.registerRenderer(ChatMessageItemRenderer())

        items.observe(
            this,
            Observer {
                val oldCount = adapter.itemCount
                adapter.updateItems(it)
                if (adapter.itemCount != oldCount) {
                    // New message, scroll to the bottom
                    binding.recyclerView.scrollToPosition(adapter.itemCount - 1)
                }
            }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)

        binding.edtMessage.onCommitContentListener = onCommitContentListener

        edtMessage.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                fab.collapse()
            }
        }

        /*
        btnRequestMoney.setOnClickListener {
            val args = Bundle()
            fab.collapse()
            args.putString(TransferFragment.ARG_PUBLIC_KEY, publicKeyBin)
            args.putString(TransferFragment.ARG_NAME, name)
            args.putBoolean(TransferFragment.ARG_IS_REQUEST, true)
            findNavController().navigate(
                R.id.action_conversationFragment_to_transferFragment,
                args
            )
        }
        */

        btnSendMoney.setOnClickListener {
            val args = Bundle()
            fab.collapse()
            args.putString(TransferFragment.ARG_PUBLIC_KEY, publicKeyBin)
            args.putString(TransferFragment.ARG_NAME, name)
            args.putBoolean(TransferFragment.ARG_IS_REQUEST, false)
            findNavController().navigate(
                R.id.action_conversationFragment_to_transferFragment,
                args
            )
        }

        btnSend.setOnClickListener {
            val message = binding.edtMessage.text.toString()
            if (message.isNotEmpty()) {
                getPeerChatCommunity().sendMessage(message, publicKey)
                binding.edtMessage.text = null
            }
        }

        btnAddImage.setOnClickListener {
            val intent = Intent()
            fab.collapse()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            @Suppress("DEPRECATION")
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        when (requestCode) {
            PICK_IMAGE -> if (resultCode == Activity.RESULT_OK && data != null) {
                val uri = data.data
                if (uri != null) {
                    sendImageFromUri(uri)
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun createItems(messages: List<ChatMessage>): List<Item> {
        return messages.mapIndexed { index, chatMessage ->
            val shouldShowAvatar = !chatMessage.outgoing && (
                index == messages.size - 1 ||
                    messages[index + 1].outgoing != chatMessage.outgoing
                )
            /*
            val shouldShowDate = (index == messages.size - 1 ||
                messages[index + 1].outgoing != chatMessage.outgoing ||
                (messages[index + 1].timestamp.time - chatMessage.timestamp.time > GROUP_TIME_LIMIT))
             */
            val shouldShowDate = true
            ChatMessageItem(
                chatMessage,
                transactionRepository.getTransactionWithHash(chatMessage.transactionHash),
                shouldShowAvatar,
                shouldShowDate,
                name
            )
        }
    }

    private fun sendImageFromUri(uri: Uri) {
        val file = saveFile(requireContext(), uri)
        getPeerChatCommunity().sendImage(file, publicKey)
    }

    companion object {
        const val ARG_PUBLIC_KEY = "public_key"
        const val ARG_NAME = "name"

        private const val GROUP_TIME_LIMIT = 60 * 1000
        private const val PICK_IMAGE = 10
    }
}
