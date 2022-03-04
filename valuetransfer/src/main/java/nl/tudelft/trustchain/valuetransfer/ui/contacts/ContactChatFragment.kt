package nl.tudelft.trustchain.valuetransfer.ui.contacts

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.TypedValue
import android.view.*
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.android.synthetic.main.dialog_image.*
import kotlinx.android.synthetic.main.fragment_contacts_chat.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.messaging.eva.TransferProgress
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.util.getColorByHash
import nl.tudelft.trustchain.common.util.viewBinding
import nl.tudelft.trustchain.peerchat.db.PeerChatStore
import nl.tudelft.trustchain.peerchat.entity.ChatMessage
import nl.tudelft.trustchain.peerchat.ui.conversation.MessageAttachment
import nl.tudelft.trustchain.peerchat.util.saveFile
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ui.VTFragment
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.databinding.FragmentContactsChatBinding
import nl.tudelft.trustchain.valuetransfer.dialogs.*
import nl.tudelft.trustchain.common.valuetransfer.entity.IdentityAttribute
import nl.tudelft.trustchain.peerchat.entity.ContactImage
import nl.tudelft.trustchain.peerchat.entity.ContactState
import nl.tudelft.trustchain.valuetransfer.ui.QRScanController
import nl.tudelft.trustchain.valuetransfer.ui.settings.AppPreferences
import nl.tudelft.trustchain.valuetransfer.util.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import androidx.recyclerview.widget.RecyclerView
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.Log
import androidx.core.widget.doAfterTextChanged
import androidx.documentfile.provider.DocumentFile
import com.google.android.gms.location.*
import nl.tudelft.ipv8.messaging.eva.TransferState
import nl.tudelft.trustchain.common.eurotoken.Transaction
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import java.math.BigInteger

class ContactChatFragment : VTFragment(R.layout.fragment_contacts_chat) {
    private val binding by viewBinding(FragmentContactsChatBinding::bind)

    private val adapterMessages = ItemAdapter()

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

    private var blocks = MutableLiveData<List<TrustChainBlock>>(listOf())

    private var oldMessageCount: Int = 0
    private var newMessageCount: Int = 0

    private val itemsMessages: LiveData<List<Item>> by lazy {
        combine(
            getPeerChatStore().getAllByPublicKey(publicKey),
            downloadProgress.asFlow(),
            searchFilterLimit.asFlow(),
            limitedMessageCount.asFlow(),
            blocks.asFlow()
        ) { messages, downloadTransferProgress, searchFilterLimitValues, _, blocks ->
            val filteredMessages = messages.filter { item ->
                val hasMessage = item.message.isNotEmpty()
                val hasAttachment = item.attachment != null
                val hasTransaction = item.transactionHash != null
                val messageContainsTerm = item.message.contains(searchFilterLimitValues.first, ignoreCase = true)

                fun attachmentTypeOf(type: String): Boolean {
                    return item.attachment!!.type == type
                }
                when (searchFilterLimitValues.second) {
                    FILTER_TYPE_MESSAGE -> hasMessage && !hasAttachment && !hasTransaction && messageContainsTerm
                    FILTER_TYPE_TRANSACTION -> (!hasAttachment && hasTransaction && messageContainsTerm) || (hasAttachment && !hasTransaction && attachmentTypeOf(MessageAttachment.TYPE_TRANSFER_REQUEST) && messageContainsTerm)
                    FILTER_TYPE_PHOTO -> !hasMessage && hasAttachment && !hasTransaction && attachmentTypeOf(MessageAttachment.TYPE_IMAGE)
                    FILTER_TYPE_FILE -> hasAttachment && !hasTransaction && attachmentTypeOf(MessageAttachment.TYPE_FILE)
                    FILTER_TYPE_LOCATION -> hasAttachment && !hasTransaction && attachmentTypeOf(MessageAttachment.TYPE_LOCATION) && messageContainsTerm
                    FILTER_TYPE_CONTACT -> hasAttachment && !hasTransaction && attachmentTypeOf(MessageAttachment.TYPE_CONTACT) && messageContainsTerm
                    FILTER_TYPE_IDENTITY_ATTRIBUTE -> hasAttachment && !hasTransaction && attachmentTypeOf(MessageAttachment.TYPE_IDENTITY_ATTRIBUTE) && messageContainsTerm
                    else -> messageContainsTerm
                }
            }

            totalMessageCount = filteredMessages.size
            oldMessageCount = newMessageCount
            newMessageCount = totalMessageCount
            val limitMessages = filteredMessages.takeLast(limitedMessageCount.value?.toInt()!!)

            createMessagesItems(limitMessages, downloadTransferProgress, blocks)
        }.asLiveData()
    }

    private val peers = MutableLiveData<List<Peer>>(emptyList())

    private val contactState: LiveData<ContactState?> by lazy {
        getPeerChatStore().getContactStateFlow(publicKey).asLiveData()
    }

    private val contactImage: LiveData<ContactImage?> by lazy {
        getPeerChatStore().getContactImageFlow(publicKey).asLiveData()
    }

    private val contact: LiveData<Contact?> by lazy {
        getContactStore().getContactFromPublickey(publicKey).asLiveData()
    }

    private var isConnected = false
    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)

    // Limit the number of shown messages
    private val limitedMessageCount = MutableLiveData(MESSAGES_SHOWN)
    private var messageCountChanged = false
    private var totalMessageCount = 0

    // Search and filter the messages
    private var searchFilterLimit = MutableLiveData(Pair("", FILTER_TYPE_EVERYTHING))
    private var cameraUri: Uri? = null
    private var downloadProgress: MutableLiveData<MutableMap<String, TransferProgress>> = MutableLiveData(mutableMapOf())

    init {
        setHasOptionsMenu(true)

        lifecycleScope.launchWhenCreated {
            while (isActive) {
                getTrustChainHelper().getChainByUser(trustchain.getMyPublicKey()).let { blockList ->
                    if (blockList != blocks.value) {
                        blocks.postValue(blockList)
                    }
                }
                delay(2000L)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_contacts_chat, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        parentActivity.setTheme(AppPreferences.APP_THEME)

        super.onCreate(savedInstanceState)

        getPeerChatCommunity().setEVAOnReceiveProgressCallback { _, info, progress ->
            Log.d("VTLOG", "CONTACT CHAT RECEIVE PROGRESS CALLBACK '$info': '$progress'")

            downloadProgress.value?.let { map ->
                if (!map.containsKey(progress.id) || (
                    map.containsKey(progress.id) && (
                        map[progress.id]!!.state != progress.state ||
                            kotlin.math.floor(map[progress.id]!!.progress) != kotlin.math.floor(progress.progress)
                        )
                    )
                ) {
                    map[progress.id] = progress
                    downloadProgress.postValue(map)
                }
            }
        }

        getPeerChatCommunity().setEVAOnReceiveCompleteCallback { _, info, fileID, _ ->
            Log.d("VTLOG", "CONTACT CHAT RECEIVE COMPLETE CALLBACK '$info'")

            downloadProgress.value?.let { map ->
                map.remove(fileID)
                downloadProgress.postValue(map)
            }
        }

        adapterMessages.registerRenderer(
            ContactChatItemRenderer(
                parentActivity,
                {
                    val attachment = it.chatMessage.attachment
                    val transactionHash = it.chatMessage.transactionHash
                    if (attachment != null) {
                        when (attachment.type) {
                            MessageAttachment.TYPE_FILE -> {
                                val file = attachment.getFile(requireContext())

                                if (file.exists()) {
                                    val senderName = if (getTrustChainCommunity().myPeer.publicKey == publicKey) {
                                        resources.getString(R.string.text_you)
                                    } else getContactStore().getContactFromPublicKey(publicKey)?.name ?: resources.getString(R.string.text_unknown_contact)
                                    val sendDate = it.chatMessage.timestamp

                                    val chatMediaItem = ChatMediaItem(
                                        it.chatMessage.id,
                                        senderName,
                                        sendDate,
                                        MessageAttachment.TYPE_FILE,
                                        it.chatMessage.attachment!!.getFile(requireContext()),
                                        it.chatMessage.message
                                    )
                                    ChatMediaDialog(
                                        requireContext(),
                                        publicKey,
                                        chatMediaItem
                                    ).show(parentFragmentManager, tag)
                                } else {
                                    parentActivity.displayToast(
                                        requireContext(),
                                        resources.getString(R.string.snackbar_attachment_file_not_exists)
                                    )
                                }
                            }
                            MessageAttachment.TYPE_IMAGE -> {
                                val file = attachment.getFile(requireContext())
                                if (file.exists()) {
                                    val senderName = if (getTrustChainCommunity().myPeer.publicKey == publicKey) {
                                        resources.getString(R.string.text_you)
                                    } else getContactStore().getContactFromPublicKey(publicKey)?.name ?: resources.getString(R.string.text_unknown_contact)
                                    val sendDate = it.chatMessage.timestamp

                                    val chatMediaItem = ChatMediaItem(
                                        it.chatMessage.id,
                                        senderName,
                                        sendDate,
                                        MessageAttachment.TYPE_IMAGE,
                                        it.chatMessage.attachment!!.getFile(requireContext()),
                                        it.chatMessage.message
                                    )
                                    ChatMediaDialog(
                                        requireContext(),
                                        publicKey,
                                        chatMediaItem
                                    ).show(parentFragmentManager, tag)
                                } else {
                                    parentActivity.displayToast(
                                        requireContext(),
                                        resources.getString(R.string.snackbar_attachment_file_not_exists),
                                    )
                                }
                            }
                            MessageAttachment.TYPE_CONTACT -> {
                                val contact = Contact.deserialize(attachment.content, 0).first
                                val contactLocal = getContactStore().getContactFromPublicKey(contact.publicKey)

                                if (contactLocal == null) {
                                    when (contact.publicKey) {
                                        getTrustChainCommunity().myPeer.publicKey -> parentActivity.displayToast(
                                            requireContext(),
                                            resources.getString(R.string.snackbar_contact_add_error_self),
                                        )
                                        else -> {
                                            ConfirmDialog(
                                                resources.getString(
                                                    R.string.text_confirm_contact_add,
                                                    contact.name
                                                )
                                            ) { dialog ->
                                                try {
                                                    getContactStore().addContact(contact.publicKey, contact.name)
                                                    parentActivity.displayToast(
                                                        requireContext(),
                                                        resources.getString(
                                                            R.string.snackbar_contact_add_success,
                                                            contact.name
                                                        )
                                                    )

                                                    dialog.dismiss()
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }.show(parentFragmentManager, tag)
                                        }
                                    }
                                } else {
                                    goToContactFragment(contactLocal)
                                }
                            }
                            MessageAttachment.TYPE_LOCATION -> {
                                val offsetBuffer = attachment.content.copyOfRange(0, attachment.content.size)
                                JSONObject(offsetBuffer.decodeToString()).let { json ->
                                    Location("").apply {
                                        latitude =
                                            json.getDouble(resources.getString(R.string.text_latitude))
                                        longitude =
                                            json.getDouble(resources.getString(R.string.text_longitude))
                                    }.let { loc ->
                                        ChatLocationDialog(publicKey, loc).show(parentFragmentManager, tag)
                                    }
                                }
                            }
                            MessageAttachment.TYPE_IDENTITY_ATTRIBUTE -> {
                                val attributeValue = IdentityAttribute.deserialize(it.chatMessage.attachment!!.content, 0).first
                                copyToClipboard(
                                    requireContext(),
                                    attributeValue.value,
                                    resources.getString(R.string.text_title_identity_attribute)
                                )
                                parentActivity.displayToast(
                                    requireContext(),
                                    resources.getString(
                                        R.string.snackbar_copied_clipboard,
                                        resources.getString(R.string.text_title_identity_attribute)
                                    )
                                )
                            }
                            MessageAttachment.TYPE_TRANSFER_REQUEST -> {
                                val offsetBuffer = attachment.content.copyOfRange(0, attachment.content.size)
                                JSONObject(offsetBuffer.decodeToString()).let { json ->
                                    val description = if (it.chatMessage.message.isNotEmpty()) {
                                        resources.getString(
                                            R.string.text_transfer_of_request,
                                            it.chatMessage.message
                                        )
                                    } else null

                                    if (json.has(QRScanController.KEY_AMOUNT)) {
                                        val amount = json.getString(QRScanController.KEY_AMOUNT)
                                        val contact = getContactStore().getContactFromPublicKey(publicKey)

                                        if (contact != null) {
                                            ExchangeTransferMoneyDialog(
                                                contact,
                                                amount,
                                                true,
                                                description
                                            ).show(parentFragmentManager, tag)
                                        } else {
                                            parentActivity.displayToast(
                                                requireContext(),
                                                resources.getString(R.string.snackbar_transfer_error_contact_add),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (transactionHash != null) {
                        it.transaction?.let { block ->
                            val myPk = getTrustChainCommunity().myPeer.publicKey
                            val myBlocks = getTrustChainHelper().getChainByUser(myPk.keyToBin())
                            val sender = if (block.publicKey.toHex() == myPk.keyToBin().toHex()) {
                                defaultCryptoProvider.keyFromPublicBin(block.linkPublicKey)
                            } else defaultCryptoProvider.keyFromPublicBin(block.publicKey)

                            Transaction(
                                block,
                                sender,
                                sender,
                                if (block.transaction.containsKey(TransactionRepository.KEY_AMOUNT)) {
                                    (block.transaction[TransactionRepository.KEY_AMOUNT] as BigInteger).toLong()
                                } else 0L,
                                block.type,
                                !it.chatMessage.outgoing,
                                block.timestamp
                            ).toExchangeTransactionItem(myPk, myBlocks).let { item ->
                                ExchangeTransactionDialog(item).show(parentFragmentManager, ExchangeTransactionDialog.TAG)
                            }
                        }
                    }
                },
                {
                    val peer = Peer(it.first)
                    val id = it.second
                    val toggleState = getPeerChatCommunity().evaProtocol!!.toggleIncomingTransfer(peer, id)

                    downloadProgress.value?.let { map ->
                        val transferProgress = TransferProgress(
                            id,
                            toggleState,
                            0.0
                        )
                        map[id] = transferProgress
                        downloadProgress.postValue(map)
                    }
                },
                {
                    showMoreMessages()
                }
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()

        contact.observe(
            viewLifecycleOwner,
            Observer { contact ->
                val identityName = getPeerChatStore().getContactState(publicKey)?.identityInfo?.let {
                    "${it.initials} ${it.surname}"
                }
                parentActivity.setActionBarTitle(
                    contact?.name ?: (identityName ?: resources.getString(R.string.text_unknown_contact)),
                    resources.getString(
                        if (isConnected) {
                            R.string.text_contact_connected
                        } else {
                            R.string.text_tap_contact_info
                        }
                    )
                )
            }
        )

        binding.etMessage.doAfterTextChanged { state ->
            toggleButton(btnSendMessage, state != null && state.isNotEmpty())
        }

        binding.btnSendMessage.setOnClickListener {
            binding.pbSendMessage.isVisible = true
            val message = binding.etMessage.text.toString()
            etMessage.closeKeyboard(requireContext())
            binding.etMessage.text = null
            binding.etMessage.clearFocus()

            @Suppress("DEPRECATION")
            Handler().postDelayed(
                {
                    getPeerChatCommunity().sendMessage(
                        message,
                        publicKey,
                        getIdentityCommunity().getIdentityInfo(appPreferences.getIdentityFaceHash())
                    )
                    binding.pbSendMessage.isVisible = false
                },
                500
            )
        }

        binding.btnNewChatAdd.apply {
            setOnClickListener {
                addRenameContact(Contact("", publicKey))
            }
        }

        binding.btnNewChatBlock.setOnClickListener {
            toggleBlockContact(false)
            binding.clNewChatRequest.isVisible = false
        }

        lifecycleScope.launchWhenResumed {
            while (isActive) {
                peers.value = getPeerChatCommunity().getPeers()
                delay(1000L)
            }
        }

        peers.observe(
            viewLifecycleOwner,
            Observer { list ->
                val peer = list.find { it.mid == publicKey.keyToHash().toHex() }

                if ((peer != null && !peer.address.isEmpty()) || (peer?.bluetoothAddress != null)) {
                    isConnected = true
                    parentActivity.setActionBarSubTitle(resources.getString(R.string.text_contact_connected))
                    parentActivity.setActionBarSubTitleIcon(R.drawable.circle_online)
                } else {
                    isConnected = false
                    parentActivity.setActionBarSubTitle(resources.getString(R.string.text_tap_contact_info))
                    parentActivity.setActionBarSubTitleIcon(R.drawable.circle_offline)
                }
            }
        )

        binding.ivAttachment.setOnClickListener {
            OptionsDialog(
                R.menu.contact_chat_attachments,
                getString(R.string.text_choose_attachment),
                bigOptionsEnabled = true,
                bigOptionsNumber = 8,
                bigOptionsCols = 2,
            ) { _, item ->
                val contact = getContactStore().getContactFromPublicKey(publicKey)

                when (item.itemId) {
                    R.id.actionSendCamera -> {
                        if (!checkCameraPermissions()) {
                            parentActivity.displayToast(
                                requireContext(),
                                resources.getString(R.string.snackbar_camera_retrieve_error)
                            )
                        } else {
                            cameraUri = activity?.contentResolver?.insert(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                ContentValues()
                            )

                            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)
                            startActivityForResult(intent, PICK_CAMERA)
                        }
                    }
                    R.id.actionSendPhotoFromLibrary -> {
                        val mimeTypes = arrayOf("image/*")
                        val intent = Intent(Intent.ACTION_GET_CONTENT)
                        intent.type = "*/*"
                        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        startActivityForResult(
                            Intent.createChooser(
                                intent,
                                resources.getString(R.string.text_send_photo_video)
                            ),
                            PICK_IMAGE
                        )
                    }
                    R.id.actionSendFile -> {
                        val mimeTypes = arrayOf(
                            "video/*",
                            "text/plain",
                            "application/*"
                        )
                        val intent = Intent(Intent.ACTION_GET_CONTENT)
                        intent.type = "*/*"
                        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                        startActivityForResult(
                            Intent.createChooser(
                                intent,
                                resources.getString(R.string.text_send_file)
                            ),
                            PICK_FILE
                        )
                    }
                    R.id.actionSendLocation -> ChatLocationDialog(publicKey).show(parentFragmentManager, tag)
                    R.id.actionSendContact -> ContactShareDialog(
                        null,
                        contact
                    ).show(parentFragmentManager, tag)
                    R.id.actionSendIdentityAttribute -> IdentityAttributeShareDialog(
                        contact,
                        null
                    ).show(parentFragmentManager, tag)
                    R.id.actionTransferMoney -> {
                        if (contact != null) {
                            ExchangeTransferMoneyDialog(
                                contact,
                                null,
                                true
                            ).show(parentFragmentManager, tag)
                        } else {
                            parentActivity.displayToast(
                                requireContext(),
                                resources.getString(R.string.snackbar_transfer_error_contact_add)
                            )
                        }
                    }
                    R.id.actionRequestMoney -> {
                        if (contact != null) {
                            ExchangeTransferMoneyDialog(
                                contact,
                                null,
                                false
                            ).show(parentFragmentManager, tag)
                        } else {
                            parentActivity.displayToast(
                                requireContext(),
                                resources.getString(R.string.snackbar_request_error_contact_add)
                            )
                        }
                    }
                }
            }.show(parentFragmentManager, tag)
        }

        binding.rvMessages.adapter = adapterMessages
        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
            reverseLayout = false
        }

        binding.ivFilterByType.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.ic_everything
            )
        )

        binding.clFilterByType.setOnClickListener {
            OptionsDialog(
                R.menu.contact_chat_filter_types,
                resources.getString(R.string.dialog_filter_by),
                bigOptionsEnabled = true,
                bigOptionsNumber = 8,
                bigOptionsCols = 2,
            ) { _, item ->
                searchFilterLimit.value = searchFilterLimit.value?.copy(second = item.title.toString())

                binding.ivFilterByType.setImageDrawable(item.icon)
            }.show(parentFragmentManager, tag)
        }

        binding.etSearchMessage.doAfterTextChanged { searchText ->
            ivSearchClearIcon.isVisible = searchText != null && searchText.isNotEmpty()
            searchFilterLimit.value = searchFilterLimit.value?.copy(first = searchText.toString())
        }

        onFocusChange(binding.etSearchMessage, requireContext())

        itemsMessages.observe(
            viewLifecycleOwner,
            Observer { list ->

                val scrollToBottom = when {
                    adapterMessages.items.isEmpty() && list.isNotEmpty() -> true // on open fragment
                    newMessageCount > oldMessageCount -> { // on new message
                        when ((list[list.size - 1] as ContactChatItem).chatMessage.outgoing) {
                            false -> { // incoming message only scroll to bottom when at least one of the last five messages is shown
                                val layoutManager = LinearLayoutManager::class.java.cast(binding.rvMessages.layoutManager)
                                if (layoutManager?.findLastVisibleItemPosition()!! < adapterMessages.itemCount - SCROLL_BOTTOM_MESSAGES_SHOWN) {
                                    binding.tvScrollToBottomNewMessage.isVisible = true
                                    false
                                } else true
                            }
                            else -> true // outgoing message scroll to bottom
                        }
                    }
                    messageCountChanged -> false // when more messages are shown (scroll to top)
                    newMessageCount == oldMessageCount -> false // when single items are updated
                    else -> true
                }

                adapterMessages.updateItems(list)
                binding.rvMessages.setItemViewCacheSize(list.size)

                binding.clNewChatRequest.isVisible = !getPeerChatStore().getContactStateForType(
                    publicKey,
                    PeerChatStore.STATUS_BLOCK
                ) && getContactStore().getContactFromPublicKey(publicKey) == null

                if (scrollToBottom)
                    scrollToBottom(binding.rvMessages)

                messageCountChanged = false
            }
        )

        binding.rvMessages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = LinearLayoutManager::class.java.cast(recyclerView.layoutManager)
                val totalItemCount = layoutManager!!.itemCount
                val firstVisible = layoutManager.findFirstVisibleItemPosition()
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val endHasBeenReached = lastVisible + SCROLL_BOTTOM_MESSAGES_SHOWN >= totalItemCount

                binding.clScrollToBottom.isVisible = totalItemCount > 0 && !(totalItemCount > 0 && endHasBeenReached)

                if (!(totalItemCount > 0 && endHasBeenReached && !(firstVisible == 0 && lastVisible == totalItemCount - 1))) {
                    binding.tvScrollToBottomNewMessage.isVisible = false
                }
            }
        })

        binding.clScrollToBottom.setOnClickListener {
            scrollToBottom(rvMessages)
        }

        binding.ivSearchClearIcon.setOnClickListener {
            binding.etSearchMessage.text = null
            searchFilterLimit.value = searchFilterLimit.value?.copy(first = "")
        }

        binding.btnUnblockContact.setOnClickListener {
            toggleBlockContact(
                getPeerChatStore().getContactStateForType(publicKey, PeerChatStore.STATUS_BLOCK)
            )
            clNewChatRequest.isVisible = getContactStore().getContactFromPublicKey(publicKey) == null
        }

        contactState.observe(
            viewLifecycleOwner,
            Observer { contactState ->
                if (contactState == null) return@Observer

                val color = if (contactState.identityInfo?.isVerified == true) {
                    ContextCompat.getColor(requireContext(), getColorIDFromThemeAttribute(parentActivity, R.attr.colorAccent))
                } else ContextCompat.getColor(requireContext(), getColorIDFromThemeAttribute(parentActivity, R.attr.colorError))
                val icon = if (contactState.identityInfo?.isVerified == true) {
                    R.drawable.ic_verified_smaller
                } else R.drawable.ic_verified_not_smaller

                parentActivity.setActionBarTitleIcon(icon, color)

                binding.clBlockedContact.isVisible = contactState.isBlocked
                binding.clMessageInputRow.isVisible = !contactState.isBlocked
            }
        )

        contactImage.observe(
            viewLifecycleOwner,
            Observer { contactImage ->
                val contactImageView = parentActivity.getCustomActionBar().findViewById<ImageView>(R.id.ivContactImage)
                val contactIdenticonView = parentActivity.getCustomActionBar().findViewById<ImageView>(R.id.ivContactIdenticon)

                if (contactImage?.image == null) {
                    val publicKeyString = publicKey.keyToBin().toHex()
                    generateIdenticon(
                        publicKeyString.substring(20, publicKeyString.length).toByteArray(),
                        getColorByHash(requireContext(), publicKeyString),
                        resources
                    ).let {
                        contactIdenticonView.apply {
                            setImageBitmap(it)
                            isVisible = true
                        }
                        contactImageView.isVisible = false
                    }
                } else {
                    contactImageView.apply {
                        setImageBitmap(contactImage.image)
                        isVisible = true
                    }
                    contactIdenticonView.isVisible = false
                }
            }
        )

        parentActivity.getCustomActionBar().setOnClickListener {
            ContactInfoDialog(publicKey).show(parentFragmentManager, ContactInfoDialog.TAG)
        }
    }

    override fun initView() {
        super.initView()

        parentActivity.apply {
            toggleActionBar(true)
            setActionBarTitle(
                name,
                resources.getString(R.string.text_tap_contact_info)
            )
            setActionBarSubTitleIcon(R.drawable.circle_offline)
            toggleBottomNavigation(false)

            setActionBarTitleSize(resources.getDimension(R.dimen.actionBarTitleSizeChat) / resources.displayMetrics.scaledDensity)

            if (contactState.value != null) {
                val color = if (contactState.value?.identityInfo?.isVerified == true) {
                    ContextCompat.getColor(
                        requireContext(),
                        getColorIDFromThemeAttribute(parentActivity, R.attr.colorAccent)
                    )
                } else ContextCompat.getColor(
                    requireContext(),
                    getColorIDFromThemeAttribute(parentActivity, R.attr.colorError)
                )
                val icon = if (contactState.value?.identityInfo?.isVerified == true) {
                    R.drawable.ic_verified_smaller
                } else R.drawable.ic_verified_not_smaller
                setActionBarTitleIcon(icon, color)
            }

            getCustomActionBar().findViewById<CardView>(R.id.cvActionbarImage).apply {
                isVisible = true
            }
            setActionBarWithGravity(Gravity.START)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        menu.add(Menu.NONE, MENU_ITEM_SEARCH_FILTER, Menu.NONE, null)
            .setIcon(R.drawable.ic_search_filter_up)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

        menu.getItem(0).isVisible = binding.clSearchFilter.isVisible

        menu.add(Menu.NONE, MENU_ITEM_OPTIONS, Menu.NONE, null)
            .setIcon(R.drawable.ic_baseline_more_vert_24)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
    }

    @SuppressLint("RestrictedApi")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()

                return true
            }
            MENU_ITEM_SEARCH_FILTER -> {
                binding.clSearchFilter.isVisible = false

                binding.etSearchMessage.text = null
                binding.ivSearchClearIcon.isVisible = false
                searchFilterLimit.value = searchFilterLimit.value?.copy(first = "")

                if (searchFilterLimit.value?.second != FILTER_TYPE_EVERYTHING) {
                    searchFilterLimit.value = searchFilterLimit.value?.copy(second = FILTER_TYPE_EVERYTHING)
                }
                binding.ivFilterByType.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_everything
                    )
                )

                toggleSearchFilterBar(true)
                parentActivity.invalidateOptionsMenu()

                return true
            }
        }

        val contact = getContactStore().getContactFromPublicKey(publicKey)
        val contactState = getPeerChatStore().getContactState(publicKey)

        val menuId = if (contact == null) {
            R.menu.contact_chat_options_no_contact
        } else {
            R.menu.contact_chat_options_contact
        }

        OptionsDialog(
            menuId,
            resources.getString(R.string.dialog_contact_options),
            bigOptionsEnabled = true,
            bigOptionsNumber = 4,
            bigOptionsCols = 4,
            menuMods = { menu ->
                if (contactState != null) {
                    if (contactState.isArchived) {
                        menu.findItem(R.id.actionArchiveContact).title = resources.getString(R.string.menu_contact_chat_options_contact_unarchive)
                        menu.findItem(R.id.actionArchiveContact).icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_contact_unarchive)
                    }
                    if (contactState.isMuted) {
                        menu.findItem(R.id.actionMuteContact).title = resources.getString(R.string.menu_contact_chat_options_contact_unmute)
                        menu.findItem(R.id.actionMuteContact).icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_contact_unmute)
                    }
                    if (contactState.isBlocked) {
                        menu.findItem(R.id.actionBlockContact).title = resources.getString(R.string.menu_contact_chat_options_contact_unblock)
                        menu.findItem(R.id.actionBlockContact).icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_contact_unblock)
                    }
                }
                menu
            },
            optionSelected = { _, selectedItem ->
                when (selectedItem.itemId) {
                    R.id.actionViewContact -> {
                        ContactInfoDialog(publicKey).show(parentFragmentManager, ContactInfoDialog.TAG)
                    }
                    R.id.actionViewMedia -> ChatMediaDialog(
                        requireContext(),
                        publicKey,
                        null
                    ).show(parentFragmentManager, tag)
                    R.id.actionSearchFilterChat -> {
                        binding.clSearchFilter.isVisible = true
                        parentActivity.invalidateOptionsMenu()
                        toggleSearchFilterBar(false)
                    }
                    R.id.actionAddContactFromChat -> {
                        addRenameContact(
                            Contact("", publicKey)
                        )
                    }
                    R.id.actionEditContactName -> {
                        addRenameContact(contact!!)
                    }
                    R.id.actionRemoveLocalConversation -> {
                        ConfirmDialog(
                            resources.getString(
                                R.string.text_confirm_delete,
                                resources.getString(R.string.text_local_conversation)
                            )
                        ) { dialog ->
                            try {
                                getPeerChatStore().deleteMessagesOfPublicKey(publicKey)
                                getPeerChatStore().removeContactState(publicKey)

                                parentActivity.displayToast(
                                    requireContext(),
                                    resources.getString(R.string.snackbar_chat_remove_success),
                                    isShort = false
                                )

                                dialog.dismiss()

                                onBackPressed()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }.show(parentFragmentManager, tag)
                    }
                    R.id.actionRemoveContact -> {
                        ConfirmDialog(
                            resources.getString(
                                R.string.text_confirm_delete,
                                contact!!.name
                            )
                        ) { dialog ->
                            try {
                                getContactStore().deleteContact(contact)

                                parentActivity.displayToast(
                                    requireContext(),
                                    resources.getString(R.string.snackbar_contact_remove_success, contact.name),
                                    isShort = false
                                )
                                dialog.dismiss()

                                onBackPressed()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }.show(parentFragmentManager, tag)
                    }
                    R.id.actionMuteContact -> {
                        getPeerChatStore().setState(
                            publicKey,
                            PeerChatStore.STATUS_MUTE,
                            if (contactState != null) !contactState.isMuted else true
                        )
                    }
                    R.id.actionArchiveContact -> {
                        getPeerChatStore().setState(
                            publicKey,
                            PeerChatStore.STATUS_ARCHIVE,
                            if (contactState != null) !contactState.isArchived else true
                        )
                    }
                    R.id.actionBlockContact -> {
                        val isBlocked = contactState?.isBlocked ?: false

                        toggleBlockContact(isBlocked)
                    }
                    R.id.actionCopyPublicKey -> {
                        copyToClipboard(
                            requireContext(),
                            publicKey.keyToBin().toHex(),
                            resources.getString(R.string.text_public_key)
                        )

                        parentActivity.displayToast(
                            requireContext(),
                            resources.getString(
                                R.string.snackbar_copied_clipboard,
                                resources.getString(R.string.text_public_key)
                            )
                        )
                    }
                    R.id.actionShareContact -> ContactShareDialog(
                        contact,
                        null
                    ).show(parentFragmentManager, tag)
                }
            }
        ).show(parentFragmentManager, tag)

        return super.onOptionsItemSelected(item)
    }

    private fun showMoreMessages() {
        messageCountChanged = true
        limitedMessageCount.value = limitedMessageCount.value?.plus(MESSAGES_SHOW_MORE)
    }

    private fun toggleBlockContact(isBlocked: Boolean) {
        getPeerChatStore().setState(
            publicKey,
            PeerChatStore.STATUS_BLOCK,
            !isBlocked
        )

        binding.clNewChatRequest.isVisible = !getPeerChatStore().getContactStateForType(publicKey, PeerChatStore.STATUS_BLOCK) && getContactStore().getContactFromPublicKey(publicKey) == null
    }

    private fun addRenameContact(contact: Contact) {
        val dialogContactRename = ContactRenameDialog(contact).newInstance(123)
        dialogContactRename.setTargetFragment(this, 1)

        @Suppress("DEPRECATION")
        dialogContactRename.show(requireFragmentManager().beginTransaction(), "dialog")
    }

    private fun toggleSearchFilterBar(hide: Boolean = true) {
        binding.clSearchFilter.isVisible = !hide

        val typedValue = TypedValue()

        parentActivity.theme.resolveAttribute(
            if (hide) R.attr.colorPrimary else R.attr.colorAccent,
            typedValue,
            true
        )

        val color = ContextCompat.getColor(requireContext(), typedValue.resourceId)

        parentActivity.window.statusBarColor = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            val currentTheme = appPreferences.getCurrentTheme()
            if (currentTheme == AppPreferences.APP_THEME_NIGHT) {
                color
            } else {
                Color.BLACK
            }
        } else color

        val actionBarTypedValue = TypedValue()
        parentActivity.theme.resolveAttribute(
            R.attr.onBackgroundColor,
            actionBarTypedValue,
            true
        )

        val actionBarTitleColor = if (hide) {
            ContextCompat.getColor(requireContext(), actionBarTypedValue.resourceId)
        } else Color.WHITE

        val actionBarTitleDrawableColor = if (hide) {
            Color.WHITE
        } else ContextCompat.getColor(requireContext(), actionBarTypedValue.resourceId)

        parentActivity.supportActionBar!!.apply {
            setBackgroundDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    typedValue.resourceId
                )
            )

            customView.findViewById<TextView>(R.id.tv_actionbar_title).apply {
                setTextColor(actionBarTitleColor)
                compoundDrawables.forEach { drawable ->
                    drawable?.colorFilter = PorterDuffColorFilter(
                        actionBarTitleDrawableColor,
                        PorterDuff.Mode.SRC_IN
                    )
                }
            }
            customView.findViewById<TextView>(R.id.tv_actionbar_subtitle).setTextColor(actionBarTitleColor)
        }
    }

    private fun goToContactFragment(contact: Contact) {
        val contactChatFragment = ContactChatFragment()
        contactChatFragment.arguments = Bundle().apply {
            putString(ValueTransferMainActivity.ARG_PUBLIC_KEY, contact.publicKey.keyToBin().toHex())
            putString(ValueTransferMainActivity.ARG_NAME, contact.name)
            putString(ValueTransferMainActivity.ARG_PARENT, parentTag)
        }

        toggleSearchFilterBar()

        parentFragmentManager.beginTransaction().apply {
            setCustomAnimations(0, R.anim.exit_to_left)
            remove(this@ContactChatFragment)
            setCustomAnimations(R.anim.enter_from_right, 0)
            add(
                R.id.container, contactChatFragment,
                ValueTransferMainActivity.contactChatFragmentTag
            )
        }.commit()
    }

    fun onBackPressed() {
        toggleSearchFilterBar()
        parentActivity.setActionBarTitle("", null)
        etMessage.closeKeyboard(requireContext())

        val previousFragment = parentFragmentManager.fragments.filter {
            it.tag == parentTag
        }

        parentFragmentManager.beginTransaction().apply {
            setCustomAnimations(0, R.anim.exit_to_right)
            remove(this@ContactChatFragment)
            setCustomAnimations(R.anim.enter_from_left, 0)
            show(previousFragment[0])
        }.commitNowAllowingStateLoss()

        // Prepare the action bar for title only without subtitle, image and icon
        parentActivity.apply {
            setActionBarTitleSize(resources.getDimension(R.dimen.actionBarTitleSize) / resources.displayMetrics.scaledDensity)
            setActionBarTitleIcon()
            setActionBarSubTitleIcon()
            getCustomActionBar().run {
                findViewById<CardView>(R.id.cvActionbarImage)
                    .apply {
                        isVisible = false
                    }
                findViewById<ImageView>(R.id.ivContactIdenticon).apply {
                    isVisible = false
                    setImageBitmap(null)
                }
                findViewById<ImageView>(R.id.ivContactImage).apply {
                    isVisible = false
                    setImageBitmap(null)
                }
            }
            setActionBarWithGravity(Gravity.CENTER)
        }

        (previousFragment[0] as VTFragment).initView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE -> if (data != null) {
                    if (data.clipData != null) {
                        data.clipData?.let {
                            for (i in 0 until it.itemCount) {
                                val uri = it.getItemAt(i).uri
                                sendFromUri(uri, TYPE_IMAGE)
                            }
                        }
                    } else {
                        data.data?.let {
                            sendFromUri(it, TYPE_IMAGE)
                        }
                    }
                }
                PICK_FILE -> if (data != null) {
                    if (data.clipData != null) {
                        data.clipData?.let {
                            for (i in 0 until it.itemCount) {
                                val uri = it.getItemAt(i).uri
                                sendFromUri(uri, TYPE_FILE)
                            }
                        }
                    } else {
                        data.data?.let {
                            sendFromUri(it, TYPE_FILE)
                        }
                    }
                }
                PICK_CAMERA -> cameraUri?.let {
                    sendFromUri(it, TYPE_IMAGE)
                }
                RENAME_CONTACT -> if (data != null) {
                    searchFilterLimit.value = searchFilterLimit.value
                }
            }
        } else super.onActivityResult(requestCode, resultCode, data)
    }

    private fun sendFromUri(uri: Uri, type: String) {
        val file = saveFile(requireContext(), uri)

        when (type) {
            TYPE_IMAGE -> {
                getPeerChatCommunity().sendImage(
                    file,
                    publicKey,
                    getIdentityCommunity().getIdentityInfo(appPreferences.getIdentityFaceHash())
                )
            }
            TYPE_FILE -> {
                val documentFile = DocumentFile.fromSingleUri(requireContext(), uri)
                val fileName = documentFile?.name

                getPeerChatCommunity().sendFile(
                    file,
                    fileName ?: file.name,
                    publicKey,
                    getIdentityCommunity().getIdentityInfo(appPreferences.getIdentityFaceHash())
                )
            }
        }
    }

    private fun createMessagesItems(
        messages: List<ChatMessage>,
        transferProgress: MutableMap<String, TransferProgress>,
        blocks: List<TrustChainBlock>
    ): List<Item> {
        return messages.mapIndexed { index, chatMessage ->
            val progress = if (chatMessage.attachment != null && !chatMessage.attachmentFetched) {
                val attachmentID = chatMessage.attachment?.content?.toHex()

                if (transferProgress.containsKey(attachmentID)) {
                    transferProgress[attachmentID]
                } else TransferProgress(attachmentID ?: "", TransferState.SCHEDULED, 0.0)
            } else null

            ContactChatItem(
                chatMessage,
                if (chatMessage.transactionHash != null) getTransactionRepository().getTransactionWithHash(chatMessage.transactionHash) else null,
                if (chatMessage.transactionHash != null) blocks else listOf(),
                (index == 0) && (totalMessageCount >= limitedMessageCount.value?.toInt()!!),
                (index == 0) || (index > 0 && !dateFormat.format(messages[index-1].timestamp).equals(dateFormat.format(chatMessage.timestamp))),
                false,
                progress
            )
        }
    }

    private fun checkCameraPermissions(): Boolean {
        return if ((ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_CAMERA
            )
            false
        } else true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                parentActivity.displayToast(
                    requireContext(),
                    resources.getString(R.string.snackbar_permission_camera_granted)
                )
            } else {
                parentActivity.displayToast(
                    requireContext(),
                    resources.getString(R.string.snackbar_permission_denied)
                )
            }
        }
    }

    companion object {
        const val PICK_IMAGE = 11
        const val PICK_FILE = 12
        const val PICK_CAMERA = 13
        const val RENAME_CONTACT = 33

        const val MENU_ITEM_OPTIONS = 1
        const val MENU_ITEM_SEARCH_FILTER = 2

        const val TYPE_IMAGE = "image"
        const val TYPE_FILE = "file"

        const val FILTER_TYPE_EVERYTHING = "Everything"
        const val FILTER_TYPE_MESSAGE = "Message"
        const val FILTER_TYPE_TRANSACTION = "Transaction"
        const val FILTER_TYPE_PHOTO = "Photo"
        const val FILTER_TYPE_FILE = "File"
        const val FILTER_TYPE_LOCATION = "Location"
        const val FILTER_TYPE_CONTACT = "Contact"
        const val FILTER_TYPE_IDENTITY_ATTRIBUTE = "Identity Attribute"

        const val PERMISSION_CAMERA = 2

        const val MESSAGES_SHOWN = 20
        const val MESSAGES_SHOW_MORE = 10
        const val SCROLL_BOTTOM_MESSAGES_SHOWN = 5
    }
}
