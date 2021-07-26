package nl.tudelft.trustchain.valuetransfer.ui.contacts

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.dialog_image.*
import kotlinx.android.synthetic.main.fragment_contacts_chat.*
import kotlinx.coroutines.flow.map
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.peerchat.community.PeerChatCommunity
import nl.tudelft.trustchain.peerchat.db.PeerChatStore
import nl.tudelft.trustchain.peerchat.entity.ChatMessage
import nl.tudelft.trustchain.peerchat.ui.conversation.ChatMessageItem
import nl.tudelft.trustchain.peerchat.ui.conversation.MessageAttachment
import nl.tudelft.trustchain.peerchat.util.saveFile
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentContactsChatBinding
import nl.tudelft.trustchain.valuetransfer.dialogs.ContactRenameDialog
import nl.tudelft.trustchain.valuetransfer.dialogs.TransferMoneyDialog
import nl.tudelft.trustchain.valuetransfer.util.closeKeyboard
import nl.tudelft.trustchain.valuetransfer.util.copyToClipboard
import nl.tudelft.trustchain.valuetransfer.util.scrollToBottom
import nl.tudelft.trustchain.valuetransfer.util.toggleButton
import java.io.File
import java.text.SimpleDateFormat

class ContactChatFragment : BaseFragment(R.layout.fragment_contacts_chat) {
    private val binding by viewBinding(FragmentContactsChatBinding::bind)

    private val adapterMessages = ItemAdapter()

    private val peerChatStore by lazy {
        PeerChatStore.getInstance(requireContext())
    }

    private val gatewayStore by lazy {
        GatewayStore.getInstance(requireContext())
    }

    private val transactionRepository by lazy {
        TransactionRepository(getTrustChainCommunity(), gatewayStore)
    }

    private fun getPeerChatCommunity(): PeerChatCommunity {
        return getIpv8().getOverlay()
            ?: throw java.lang.IllegalStateException("PeerChatCommunity is not configured")
    }

    private val itemsMessages: LiveData<List<Item>> by lazy {
        peerChatStore.getAllByPublicKey(publicKey).map { messages ->
            createMessagesItems(messages)
        }.asLiveData()
    }

    private val publicKey by lazy {
        defaultCryptoProvider.keyFromPublicBin(publicKeyBin.hexToBytes())
    }

    private val publicKeyBin by lazy {
        requireArguments().getString(ValueTransferMainActivity.ARG_PUBLIC_KEY)!!
    }

    private val name by lazy {
        requireArguments().getString(ValueTransferMainActivity.ARG_NAME)!!
    }

    private val parentTag by lazy {
        requireArguments().getString(ValueTransferMainActivity.ARG_PARENT)!!
    }

    init {
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_contacts_chat, container, false)
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as ValueTransferMainActivity).toggleActionBar(true)
        (requireActivity() as ValueTransferMainActivity).setActionBarTitle(name)
        (requireActivity() as ValueTransferMainActivity).toggleBottomNavigation(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapterMessages.registerRenderer(ContactChatItemRenderer {
            val attachment = it.chatMessage.attachment
            if(attachment != null && attachment.type == MessageAttachment.TYPE_IMAGE) {
                val file = attachment.getFile(requireContext())
                if(file.exists()) {
                    dialogImage(file)
                }else{
                    Toast.makeText(requireContext(), "File does not exists", Toast.LENGTH_SHORT).show()
                }
            }
        })

        itemsMessages.observe(
            this,
            Observer {
                adapterMessages.updateItems(it)
                binding.rvMessages.setItemViewCacheSize(it.size)
                scrollToBottom(binding.rvMessages)
            }
        )
    }

    private fun dialogImage(file: File) {
        val dialog = Dialog(requireContext())
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(
            layoutInflater.inflate(
                R.layout.dialog_image, null
            )
        )
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        dialog.ivImageFullScreen.setImageBitmap(bitmap)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onResume()

        etMessage.doAfterTextChanged { state ->
            toggleButton(btnSendMessage, state != null && state.isNotEmpty())
        }

        btnSendMessage.setOnClickListener {
            getPeerChatCommunity().sendMessage(binding.etMessage.text.toString(), publicKey)
            binding.etMessage.text = null
            binding.etMessage.clearFocus()
            closeKeyboard(requireContext(), etMessage)
        }

        val optionsMenuButton = binding.ivAttachment
        optionsMenuButton.setOnClickListener {
            val optionsMenu = PopupMenu(requireContext(), optionsMenuButton)
            optionsMenu.menuInflater.inflate(R.menu.contact_chat_attachments, optionsMenu.menu)
            optionsMenu.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->

                val contact = peerChatStore.contactsStore.getContactFromPublicKey(publicKey)

                when(item.itemId) {
                    R.id.actionSendPhotoVideo -> {
                        val intent = Intent()
                        intent.type = "image/*"
                        intent.action = Intent.ACTION_GET_CONTENT
                        startActivityForResult(Intent.createChooser(intent, "Send Photo or Video"), PICK_IMAGE)
                        Toast.makeText(requireContext(), "Send photo or video", Toast.LENGTH_SHORT).show()
                    }
                    R.id.actionSendFile -> Toast.makeText(requireContext(), "Send file", Toast.LENGTH_SHORT).show()
                    R.id.actionSendLocation -> Toast.makeText(requireContext(), "Send location", Toast.LENGTH_SHORT).show()
                    R.id.actionSendContact -> Toast.makeText(requireContext(), "Send contact", Toast.LENGTH_SHORT).show()
                    R.id.actionTransferMoney -> {
                        if(contact != null) {
                            TransferMoneyDialog(contact, true, transactionRepository, getPeerChatCommunity()).show(parentFragmentManager, tag)
                        } else {
                            Toast.makeText(requireContext(), "Please add contact first to transfer money", Toast.LENGTH_SHORT).show()
                        }
                    }
                    R.id.actionRequestMoney -> {
                        if(contact != null) {
                            TransferMoneyDialog(contact, false, transactionRepository, getPeerChatCommunity()).show(parentFragmentManager, tag)
                        } else {
                            Toast.makeText(requireContext(), "Please add contact first to request money", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                true
            })
            optionsMenu.show()
        }

        val linearLayoutManager = LinearLayoutManager(requireContext())
        linearLayoutManager.stackFromEnd = true
        linearLayoutManager.reverseLayout = false

        binding.rvMessages.adapter = adapterMessages
        binding.rvMessages.layoutManager = linearLayoutManager
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.contact_chat_options, menu)

        if(peerChatStore.contactsStore.getContactFromPublicKey(publicKey) == null) {
            menu.getItem(0).title = "Add contact"
            menu.getItem(1).title = "Remove local conversation"
        }
    }

    private fun onBackPressed() {
        val previousFragment = parentFragmentManager.fragments.filter {
            it.tag == parentTag
        }

        parentFragmentManager.beginTransaction()
            .show(previousFragment[0])
            .remove(this)
            .commit()
        previousFragment[0].onResume()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var contact = peerChatStore.contactsStore.getContactFromPublicKey(publicKey)

        when(item.itemId) {
            android.R.id.home -> {
                onBackPressed()

                return true
            }
            R.id.actionEditContactName -> {
                if (contact == null) {
                    contact = Contact("", publicKey)
                }

                val dialogContactRename = ContactRenameDialog(contact).newInstance(123)
                dialogContactRename!!.setTargetFragment(this, 1)

                @Suppress("DEPRECATION")
                dialogContactRename.show(requireFragmentManager().beginTransaction(), "dialog")
            }
            R.id.actionRemoveContact -> {
                if(contact == null) {
                    for(chatItem in adapterMessages.items) {
                        peerChatStore.deleteMessage(chatItem as ChatMessageItem)
                    }
                    Toast.makeText(requireContext(), "Local conversation with contact has been removed", Toast.LENGTH_SHORT).show()
                    onBackPressed()
                }else{
                    peerChatStore.contactsStore.deleteContact(contact)
                    Toast.makeText(requireContext(), "Contact removed and chat is moved to hidden chats", Toast.LENGTH_LONG).show()
                    onBackPressed()
                }
            }
            R.id.actionCopyPublicKey -> {
                copyToClipboard(requireContext(), (contact?.publicKey ?: publicKey).keyToBin().toHex(), "Public Key")
                Toast.makeText(requireContext(), "Public key copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            PICK_IMAGE -> if (resultCode == Activity.RESULT_OK && data != null) {
                val uri = data.data
                if (uri != null) {
                    sendImageFromUri(uri)
                }
            }
            RENAME_CONTACT -> if(resultCode == Activity.RESULT_OK && data != null)  {
                val newName = data.data.toString()
                (activity as ValueTransferMainActivity).setActionBarTitle(newName)
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        closeKeyboard(requireContext(), etMessage)
    }

    private fun sendImageFromUri(uri: Uri) {
        val file = saveFile(requireContext(), uri)
        getPeerChatCommunity().sendImage(file, publicKey)
    }

    private fun createMessagesItems(messages: List<ChatMessage>): List<Item> {
        return messages.mapIndexed { index, chatMessage ->
            var shouldShowDate = false

            if(index == 0) {
                shouldShowDate = true
            }else if (index > 0) {
                val dateFormat = SimpleDateFormat("dd-MM-yyyy")
                val previousDate = dateFormat.format(messages[index-1].timestamp)
                val currentDate = dateFormat.format(chatMessage.timestamp)

                shouldShowDate = !previousDate.equals(currentDate)
            }

            ChatMessageItem(
                chatMessage,
                transactionRepository.getTransactionWithHash(chatMessage.transactionHash),
                false,
                shouldShowDate,
                name
            )
        }
    }

    companion object {
        private const val PICK_IMAGE = 10
        const val RENAME_CONTACT = 33
    }
}
