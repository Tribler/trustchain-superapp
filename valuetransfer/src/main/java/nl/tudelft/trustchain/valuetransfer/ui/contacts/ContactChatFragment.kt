package nl.tudelft.trustchain.valuetransfer.ui.contacts

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
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
import androidx.core.widget.doAfterTextChanged
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.recyclerview.widget.RecyclerView
import android.graphics.PorterDuff

import android.graphics.PorterDuffColorFilter
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import nl.tudelft.ipv8.messaging.eva.TransferState

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

    private var blocks: List<TrustChainBlock> = emptyList()

    private var oldMessageCount: Int = 0
    private var newMessageCount: Int = 0

    private val itemsMessages: LiveData<List<Item>> by lazy {
        combine(
            getPeerChatStore().getAllByPublicKey(publicKey),
            downloadProgress.asFlow(),
            searchFilterLimit.asFlow(),
            limitedMessageCount.asFlow(),
        ) { messages, downloadTransferProgress, searchFilterLimitValues, _ ->
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

            createMessagesItems(limitMessages, downloadTransferProgress)
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
                blocks = getTrustChainHelper().getChainByUser(trustchain.getMyPublicKey())
                delay(2000)
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
                if (map[progress.id] != progress) {
                    map[progress.id] = progress
                    downloadProgress.postValue(map)
                }
            }
        }

        getPeerChatCommunity().setEVAOnReceiveCompleteCallback { _, info, fileID, _ ->
            Log.d("VTLOG", "CONTACT CHAT SEND COMPLETE CALLBACK '$info'")

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
//                                val fileName = it.chatMessage.message
//
//                                if (storageIsWritable()) {
//                                     Log.d("VTLOG", "FILE ID: ${file.name}")
//                                    saveDocument(requireContext(), file, fileName)
//                                } else {
//                                    Log.d("VTLOG", "FAILED TO SAVE DOCUMENT BECAUSE OF EXTERNAL ...")
//                                    Toast.makeText(requireContext(), "Storage is not writable", Toast.LENGTH_SHORT).show()
//                                }

                                if (file.exists()) {
                                    val senderName = if (getTrustChainCommunity().myPeer.publicKey == publicKey) {
                                        "You"
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
                                    parentActivity.displaySnackbar(
                                        requireContext(),
                                        resources.getString(R.string.snackbar_attachment_file_not_exists),
                                        type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR
                                    )
                                }
                            }
                            MessageAttachment.TYPE_IMAGE -> {
                                val file = attachment.getFile(requireContext())
                                if (file.exists()) {
//                                    openImage(file)
                                    val senderName = if (getTrustChainCommunity().myPeer.publicKey == publicKey) {
                                        "You"
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
                                    parentActivity.displaySnackbar(
                                        requireContext(),
                                        resources.getString(R.string.snackbar_attachment_file_not_exists),
                                        type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR
                                    )
                                }
                            }
                            MessageAttachment.TYPE_CONTACT -> {
                                val contact = Contact.deserialize(attachment.content, 0).first

                                if (getContactStore().getContactFromPublicKey(contact.publicKey) == null) {
                                    when (contact.publicKey) {
                                        getTrustChainCommunity().myPeer.publicKey -> parentActivity.displaySnackbar(
                                            requireContext(),
                                            resources.getString(R.string.snackbar_contact_add_error_self),
                                            type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR
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
                                                    parentActivity.displaySnackbar(
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
                                    goToContactFragment(contact)
                                }
                            }
                            MessageAttachment.TYPE_LOCATION -> {
                                val offsetBuffer = attachment.content.copyOfRange(0, attachment.content.size)
                                JSONObject(offsetBuffer.decodeToString()).let { json ->
                                    val latitude = json.getString(resources.getString(R.string.text_latitude))
                                    val longitude = json.getString(resources.getString(R.string.text_longitude))

                                    val browserIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(
                                            resources.getString(
                                                R.string.text_url_google_maps,
                                                latitude,
                                                longitude
                                            )
                                        )
                                    )
                                    startActivity(browserIntent)
                                }
                            }
                            MessageAttachment.TYPE_IDENTITY_ATTRIBUTE -> {
                                val attributeValue = IdentityAttribute.deserialize(it.chatMessage.attachment!!.content, 0).first
                                copyToClipboard(
                                    requireContext(),
                                    attributeValue.value,
                                    resources.getString(R.string.text_title_identity_attribute)
                                )
                                parentActivity.displaySnackbar(
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
                                            parentActivity.displaySnackbar(
                                                requireContext(),
                                                resources.getString(R.string.snackbar_transfer_error_contact_add),
                                                type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (transactionHash != null) {
                        val transaction = getTransactionRepository().getTransactionWithHash(transactionHash)
                        val key = defaultCryptoProvider.keyFromPublicBin(transaction!!.linkPublicKey)
                        val peer = Peer(key)
                        getTransactionRepository().trustChainCommunity.sendBlock(transaction, peer)
                    }
                },
                {
                    showMoreMessages()
//                    messageCountChanged = true
//                    limitedMessageCount.value = limitedMessageCount.value?.plus(MESSAGES_SHOW_MORE)
                }
            )
        )
    }

    private fun showMoreMessages() {
        messageCountChanged = true
        limitedMessageCount.value = limitedMessageCount.value?.plus(MESSAGES_SHOW_MORE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()

        contact.observe(
            viewLifecycleOwner,
            Observer {
                parentActivity.setActionBarTitle(
                    it?.name ?: resources.getString(R.string.text_unknown_contact),
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
                } else {
                    isConnected = false
                    parentActivity.setActionBarSubTitle(resources.getString(R.string.text_tap_contact_info))
                }
            }
        )

        binding.ivAttachment.setOnClickListener {
            OptionsDialog(
                R.menu.contact_chat_attachments,
                getString(R.string.text_choose_attachment)
            ) { _, item ->
                val contact = getContactStore().getContactFromPublicKey(publicKey)

                when (item.itemId) {
                    R.id.actionSendCamera -> {
                        if (!checkCameraPermissions()) {
                            parentActivity.displaySnackbar(
                                requireContext(),
                                resources.getString(R.string.snackbar_camera_retrieve_error),
                                type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR
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
                            "application/pdf",
                            "video/*"
                        )
                        val intent = Intent(Intent.ACTION_GET_CONTENT)
                        intent.type = "*/*"
                        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                        startActivityForResult(
                            Intent.createChooser(
                                intent,
                                resources.getString(R.string.text_send_file)
                            ),
                            PICK_FILE
                        )
                    }
                    R.id.actionSendLocation -> {
                        if (!checkLocationPermissions()) {
                            parentActivity.displaySnackbar(
                                requireContext(),
                                resources.getString(R.string.snackbar_location_retrieve_error),
                                type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR
                            )
                        } else {
                            val location = getLocation()

                            if (location != null) {
                                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                                val address = geocoder.getFromLocation(
                                    location.latitude,
                                    location.longitude,
                                    1
                                )

                                var addressLine = resources.getString(
                                    R.string.text_location_address_line,
                                    location.latitude.toString(),
                                    location.longitude.toString()
                                )

                                try {
                                    addressLine = address[0].getAddressLine(0)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                                getPeerChatCommunity().sendLocation(
                                    location,
                                    addressLine,
                                    publicKey,
                                    getIdentityCommunity().getIdentityInfo(appPreferences.getIdentityFaceHash())
                                )
                            } else {
                                parentActivity.displaySnackbar(
                                    requireContext(),
                                    resources.getString(R.string.snackbar_location_no_location_error),
                                    type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR
                                )
                            }
                        }
                    }
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
                            parentActivity.displaySnackbar(
                                requireContext(),
                                resources.getString(R.string.snackbar_transfer_error_contact_add),
                                type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR
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
                            parentActivity.displaySnackbar(
                                requireContext(),
                                resources.getString(R.string.snackbar_request_error_contact_add),
                                type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR
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
                "Filter by"
            ) { _, item ->
                searchFilterLimit.value = searchFilterLimit.value?.copy(second = item.title.toString())

//                filterType.value = item.title.toString()
                binding.ivFilterByType.setImageDrawable(item.icon)
            }.show(parentFragmentManager, tag)
        }

        binding.etSearchMessage.doAfterTextChanged { searchText ->
            ivSearchClearIcon.isVisible = searchText != null && searchText.isNotEmpty()
            searchFilterLimit.value = searchFilterLimit.value?.copy(first = searchText.toString())
//            searchTerm.value = searchText.toString()
        }

        onFocusChange(binding.etSearchMessage, requireContext())

        itemsMessages.observe(
            viewLifecycleOwner,
            Observer { list ->

                val scrollToBottom = when {
                    adapterMessages.items.isEmpty() && list.isNotEmpty() -> true // on open fragment
                    newMessageCount > oldMessageCount -> { // on new message
                        when ((list[list.size-1] as ContactChatItem).chatMessage.outgoing) {
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

//                binding.clNewChatRequest.isVisible = getContactStore().getContactFromPublicKey(publicKey) == null && list.none {
//                    (it as ContactChatItem).chatMessage.sender == getTrustChainCommunity().myPeer.publicKey
//                }

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
                val topHasBeenReached = firstVisible + 1 <= 2
                val endHasBeenReached = lastVisible + SCROLL_BOTTOM_MESSAGES_SHOWN >= totalItemCount

                binding.clScrollToBottom.isVisible = totalItemCount > 0 && !(totalItemCount > 0 && endHasBeenReached)

                if (!(totalItemCount > 0 && endHasBeenReached && !(firstVisible == 0 && lastVisible == totalItemCount - 1))) {
                    binding.tvScrollToBottomNewMessage.isVisible = false
                }

                if (topHasBeenReached && newMessageCount >= limitedMessageCount.value!!)
//                if (topHasBeenReached && newMessageCount >= searchFilterLimit.value!!.third)
                    showMoreMessages()
            }
        })

        binding.clScrollToBottom.setOnClickListener {
            scrollToBottom(rvMessages)
        }

//        lifecycleScope.launchWhenCreated {
//            while (isActive) {
//                downloadingImages.forEach {
//                    val file = MessageAttachment.getFile(requireContext(), it)
//                    if (file.exists()) {
////                        imageDownloadedTrigger.postValue(true)
//                        downloadingImages.remove(it)
//
//                        val transferProgressMap = parentActivity.getAttachmentTransferProgress().value
//                        transferProgressMap?.remove(Pair(getTrustChainCommunity().myPeer.key, it))
//
//                        if (transferProgressMap != null) {
//                            parentActivity.setAttachmentTransferProgress(transferProgressMap)
//                        }
//
//                        adapterMessages.notifyDataSetChanged()
//                    }
//                }
//
//                delay(1000)
//            }
//        }

        binding.ivSearchClearIcon.setOnClickListener {
            binding.etSearchMessage.text = null
            searchFilterLimit.value = searchFilterLimit.value?.copy(first = "")
//            searchTerm.value = ""
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

                binding.clBlockedContact.isVisible = contactState.isBlocked
                binding.clMessageInputRow.isVisible = !contactState.isBlocked

                parentActivity.setActionBarTitleIcon(if (contactState.identityInfo?.isVerified == true) R.drawable.ic_verified_smaller else null)
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
            ContactInfoDialog(publicKey).show(parentFragmentManager, tag)
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
            toggleBottomNavigation(false)

            setActionBarTitleSize(resources.getDimension(R.dimen.actionBarTitleSizeChat) / resources.displayMetrics.scaledDensity)
            setActionBarTitleIcon()
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
//                searchTerm.value = ""
                if (searchFilterLimit.value?.second != FILTER_TYPE_EVERYTHING) {
                    searchFilterLimit.value = searchFilterLimit.value?.copy(second = FILTER_TYPE_EVERYTHING)
//                    filterType.value = FILTER_TYPE_EVERYTHING
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

        val menuId = if (getContactStore().getContactFromPublicKey(publicKey) == null) {
            R.menu.contact_chat_options_no_contact
        } else {
            R.menu.contact_chat_options_contact
        }

        val contact = getContactStore().getContactFromPublicKey(publicKey)
        val contactState = getPeerChatStore().getContactState(publicKey)

        OptionsDialog(
            menuId,
            "Choose Option",
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
                        ContactInfoDialog(publicKey).show(parentFragmentManager, tag)
                    }
                    R.id.actionViewMedia -> ChatMediaDialog(
                        requireContext(),
                        publicKey,
                        null
                    ).show(parentFragmentManager, tag)
//                    R.id.actionContactConnectInvitation -> {
//                        val contactConnectPayload = ContactConnectPayload(
//                            getTrustChainCommunity().myPeer.publicKey,
//                            getIdentityCommunity().getIdentityInfo(null)!!
//                        )
//                        getPeerChatCommunity().sendContactConnect(
//                            publicKey,
//                            contactConnectPayload,
//                            PeerChatCommunity.MessageId.CONTACT_CONNECT_REQUEST
//                        )
//                    }
//                    R.id.actionContactConnectConfirmation -> {
//                        val contactConnectPayload = ContactConnectPayload(
//                            getTrustChainCommunity().myPeer.publicKey,
//                            getIdentityCommunity().getIdentityInfo(null)!!
//                        )
//                        getPeerChatCommunity().sendContactConnect(
//                            publicKey,
//                            contactConnectPayload,
//                            PeerChatCommunity.MessageId.CONTACT_CONNECT
//                        )
//                    }
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
                                parentActivity.displaySnackbar(
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
                                getPeerChatStore().removeContactState(contact.publicKey)
                                getPeerChatStore().removeContactImage(contact.publicKey)
                                parentActivity.displaySnackbar(
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

                        parentActivity.displaySnackbar(
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

    private fun checkLocationPermissions(): Boolean {
        if ((ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
            (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_LOCATION)

            return false
        }

        return true
    }

    private fun checkCameraPermissions(): Boolean {
        if ((ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                PERMISSION_CAMERA
            )

            return false
        }

        return true
    }

    @SuppressLint("MissingPermission")
    private fun getLocation(): Location? {
        val locationManager = parentActivity.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
            val location = locationManager.getLastKnownLocation(provider)
            if (location != null) {
                return location
            }
        }

        return null
    }

    private fun openImage(file: File) {
        Dialog(requireContext()).apply {
            window?.requestFeature(Window.FEATURE_NO_TITLE)
            setContentView(
                layoutInflater.inflate(
                    R.layout.dialog_image,
                    null
                )
            )

            BitmapFactory.decodeFile(file.absolutePath).let {
                this.ivImageFullScreen.setImageBitmap(it)
            }

            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }.show()
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
                    data.data?.let { uri ->
                        sendFromUri(uri, TYPE_IMAGE)
                    }
                }
                PICK_FILE -> if (data != null) {
                    data.data?.let { uri ->
                        Log.d("VTLOG", "URI: $uri")
                        sendFromUri(uri, TYPE_FILE)
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

//                if (fileName.isNullOrEmpty() || fileName == file.name) {
//                    Toast.makeText(requireContext(), "File couldn't be sent", Toast.LENGTH_SHORT).show()
//                    return
//                } else if (fileName.getExtension() == null) {
//                    Toast.makeText(requireContext(), "The file should have an extension", Toast.LENGTH_SHORT).show()
//                    return
//                } else if (fileName.getMimeTypeFromExtension() == null) {
//                    Toast.makeText(requireContext(), "The MIME-type of the file couldn't be determined", Toast.LENGTH_SHORT).show()
//                    return
//                }

                getPeerChatCommunity().sendFile(
                    file,
                    fileName ?: file.name,
                    publicKey,
                    getIdentityCommunity().getIdentityInfo(appPreferences.getIdentityFaceHash())
                )
            }
        }
    }

    fun getFilename(context: Context, uri: Uri): String? {
        return when(uri.scheme) {
            ContentResolver.SCHEME_CONTENT -> {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val name = cursor.getString(nameIndex)
                    cursor.close()
                    name
                }
            }
            ContentResolver.SCHEME_FILE-> {
                uri.path?.let { path ->
                    File(path).name
                }
            }
            else -> "unknown"
        }
    }

    private fun createMessagesItems(
        messages: List<ChatMessage>,
        transferProgress: MutableMap<String, TransferProgress>
    ): List<Item> {
        return messages.mapIndexed { index, chatMessage ->
//            val attachmentID = if (chatMessage.attachment != null && !chatMessage.attachmentFetched) {
//                chatMessage.attachment?.content?.toHex()
//            } else null

            val progress = if (chatMessage.attachment != null) {
                if (!chatMessage.attachmentFetched) {
                    val attachmentID = chatMessage.attachment?.content?.toHex()

                    if (transferProgress.containsKey(attachmentID)) {
                        transferProgress[attachmentID]
                    } else TransferProgress(attachmentID ?: "", TransferState.SCHEDULED, 0.0)
                } else null
            } else null
//
//
//
//                if (chatMessage.attachmentFetched || attachmentTransferProgress[attachmentID]?.state == TransferState.FINISHED) {
//                    TransferProgress(attachmentID, TransferState.FINISHED, 100.0)
//                } else if (!chatMessage.attachmentFetched && !attachmentTransferProgress.containsKey(attachmentID)) {
//
//                } else {
//                    if (attachmentTransferProgress.containsKey(attachmentID)) {
//                        attachmentTransferProgress[attachmentID]
//                    } else {
//                        TransferProgress(attachmentID, TransferState.INITIALIZING, 0.0)
//                    }
//                }
//            } else null

//            val transferProgress = if (attachmentID != null) {
//                if (chatMessage.attachmentFetched || attachmentTransferProgress[attachmentID]?.state == TransferState.FINISHED) {
//                    TransferProgress(attachmentID, TransferState.FINISHED, 100.0)
//                } else if (!chatMessage.attachmentFetched && !attachmentTransferProgress.containsKey(attachmentID)) {
//
//                } else {
//                    if (attachmentTransferProgress.containsKey(attachmentID)) {
//                        attachmentTransferProgress[attachmentID]
//                    } else {
//                        TransferProgress(attachmentID, TransferState.INITIALIZING, 0.0)
//                    }
//                }
//            } else null

            ContactChatItem(
                chatMessage,
                getTransactionRepository().getTransactionWithHash(chatMessage.transactionHash),
                false, //                (index == 0) && (totalMessageCount > searchFilterLimit.value?.third!!), // (index == 0) && (totalMessageCount >= limitedMessageCount.value?.toInt()!!),
                (index == 0) || (index > 0 && !dateFormat.format(messages[index-1].timestamp).equals(dateFormat.format(chatMessage.timestamp))),
                false,
                progress
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                parentActivity.displaySnackbar(
                    requireContext(),
                    resources.getString(R.string.snackbar_permission_location_granted)
                )
            } else {
                parentActivity.displaySnackbar(
                    requireContext(),
                    resources.getString(R.string.snackbar_permission_denied),
                    type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR
                )
            }
        } else if (requestCode == PERMISSION_CAMERA) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                parentActivity.displaySnackbar(
                    requireContext(),
                    resources.getString(R.string.snackbar_permission_camera_granted)
                )
            } else {
                parentActivity.displaySnackbar(
                    requireContext(),
                    resources.getString(R.string.snackbar_permission_denied),
                    type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR
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

        const val PERMISSION_LOCATION = 1
        const val PERMISSION_CAMERA = 2

        const val MESSAGES_SHOWN = 20
        const val MESSAGES_SHOW_MORE = 10
        const val SCROLL_BOTTOM_MESSAGES_SHOWN = 5
    }
}
