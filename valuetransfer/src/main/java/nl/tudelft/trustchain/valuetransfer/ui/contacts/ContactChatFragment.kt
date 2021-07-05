package nl.tudelft.trustchain.valuetransfer.ui.contacts

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.dialog_image.*
import kotlinx.android.synthetic.main.fragment_contact_chat.*
import kotlinx.coroutines.flow.map
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.GatewayStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.ui.BaseFragment
import nl.tudelft.trustchain.common.util.closeKeyboard
import nl.tudelft.trustchain.common.util.copyToClipboard
import nl.tudelft.trustchain.common.util.scrollToBottom
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.peerchat.community.PeerChatCommunity
import nl.tudelft.trustchain.peerchat.db.PeerChatStore
import nl.tudelft.trustchain.peerchat.entity.ChatMessage
import nl.tudelft.trustchain.peerchat.ui.conversation.ChatMessageItem
import nl.tudelft.trustchain.peerchat.ui.conversation.ConversationFragment
import nl.tudelft.trustchain.peerchat.ui.conversation.MessageAttachment
import nl.tudelft.trustchain.peerchat.util.saveFile
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentContactChatBinding
import nl.tudelft.trustchain.valuetransfer.dialogs.ContactRenameDialog
import java.io.File
import java.text.SimpleDateFormat


class ContactChatFragment : BaseFragment(R.layout.fragment_contact_chat) {
    private val binding by viewBinding(FragmentContactChatBinding::bind)

    private val adapter = ItemAdapter()

    private val peerChatStore by lazy {
        PeerChatStore.getInstance(requireContext())
    }

    private val items: LiveData<List<Item>> by lazy {
        peerChatStore.getAllByPublicKey(publicKey).map { messages ->
            createItems(messages)
        }.asLiveData()
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

    private val publicKey by lazy {
        defaultCryptoProvider.keyFromPublicBin(publicKeyBin.hexToBytes())
    }

    private val publicKeyBin by lazy {
        requireArguments().getString(ConversationFragment.ARG_PUBLIC_KEY)!!
    }

    val name by lazy {
        requireArguments().getString(ConversationFragment.ARG_NAME)!!
    }
//
//    val name by lazy {
//        when(peerChatStore.contactsStore.getContactFromPublicKey(defaultCryptoProvider.keyFromPublicBin(publicKeyBin.hexToBytes()))!!.name.isNullOrEmpty()) {
//            true -> "Unknown"
//            false -> peerChatStore.contactsStore.getContactFromPublicKey(defaultCryptoProvider.keyFromPublicBin(publicKeyBin.hexToBytes()))!!.name
//        }
//    }

    init {
        setHasOptionsMenu(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter.registerRenderer(ContactChatItemRenderer {
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

        items.observe(
            this,
            Observer {
                adapter.updateItems(it)
                binding.rvMessages.setItemViewCacheSize(adapter.itemCount)
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

        activity?.findViewById<BottomNavigationView>(R.id.bottomNavigation)!!.isVisible = false

        etMessage.doAfterTextChanged { state ->
            btnSendMessage.isEnabled = state != null && state.isNotEmpty()
            btnSendMessage.alpha = if(state != null && state.isNotEmpty()) 1f else 0.5f
            btnSendMessage.isClickable = state != null && state.isNotEmpty()
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
                }
                true
            })
            optionsMenu.show()
        }

        val linearLayoutManager = LinearLayoutManager(requireContext())
        linearLayoutManager.stackFromEnd = true
        linearLayoutManager.reverseLayout = false

        binding.rvMessages.adapter = adapter
        binding.rvMessages.layoutManager = linearLayoutManager
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.contact_chat_options, menu)

        if(peerChatStore.contactsStore.getContactFromPublicKey(defaultCryptoProvider.keyFromPublicBin(publicKeyBin.hexToBytes())) == null) {
            menu.getItem(0).title = "Remove chat"
            menu.getItem(1).isVisible = false
            menu.getItem(2).isVisible = false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val contact = peerChatStore.contactsStore.getContactFromPublicKey(defaultCryptoProvider.keyFromPublicBin(publicKeyBin.hexToBytes()))

        when(item.itemId) {
            R.id.actionEditContactName -> {

                when(contact != null) {
                    true -> {
                        val dialogContactRename = ContactRenameDialog(contact).newInstance(123)
                        dialogContactRename!!.setTargetFragment(this, 1)

                        @Suppress("DEPRECATION")
                        dialogContactRename.show(requireFragmentManager().beginTransaction(), "dialog")
                    }
                    else -> {
                        for(chatItem in adapter.items) {
                            peerChatStore.deleteMessage(chatItem as ChatMessageItem)
                        }
                        findNavController().popBackStack(requireParentFragment().id, false)
                    }
                }

//                dialogContactRename.setTargetFragment(this, 33)
//                val transaction = childFragmentManager.beginTransaction()
//                dialogContactRename.show(transaction, "dialog_rename")
//                dialogContactRename.show(parentFragmentManager, tag)
            }
            R.id.actionRemoveContact -> {
                peerChatStore.contactsStore.deleteContact(contact!!)
                activity?.invalidateOptionsMenu()
                Toast.makeText(requireContext(), "Contact removed", Toast.LENGTH_SHORT).show()
            }
            R.id.actionCopyPublicKey -> {
                copyToClipboard(requireContext(), contact!!.publicKey.keyToBin().toHex(), "Public Key")
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
//        activity?.findViewById<BottomNavigationView>(R.id.bottomNavigation)!!.isVisible = true
    }

    private fun sendImageFromUri(uri: Uri) {
        val file = saveFile(requireContext(), uri)
        getPeerChatCommunity().sendImage(file, publicKey)
    }

    private fun createItems(messages: List<ChatMessage>): List<Item> {
        return messages.mapIndexed { index, chatMessage ->
            var shouldShowDate = true

            if(index > 0) {
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
        const val ARG_PUBLIC_KEY = "public_key"
        const val ARG_NAME = "name"

        private const val PICK_IMAGE = 10
        const val RENAME_CONTACT = 33
    }
}
