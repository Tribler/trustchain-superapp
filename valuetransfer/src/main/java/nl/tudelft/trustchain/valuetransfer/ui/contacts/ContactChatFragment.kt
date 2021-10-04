package nl.tudelft.trustchain.valuetransfer.ui.contacts

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
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
import android.widget.TextView
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
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.Contact
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
import nl.tudelft.trustchain.valuetransfer.entity.IdentityAttribute
import nl.tudelft.trustchain.valuetransfer.ui.QRScanController
import nl.tudelft.trustchain.valuetransfer.ui.settings.AppPreferences
import nl.tudelft.trustchain.valuetransfer.util.*
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ContactChatFragment : VTFragment(R.layout.fragment_contacts_chat) {
    private val binding by viewBinding(FragmentContactsChatBinding::bind)

    private val adapterMessages = ItemAdapter()

    private var blocks: List<TrustChainBlock> = emptyList()

    private val itemsMessages: LiveData<List<Item>> by lazy {
        combine(getPeerChatStore().getAllByPublicKey(publicKey), limitedMessageCount.asFlow(), searchTerm.asFlow(), filterType.asFlow()) { messages, _, _, _ ->
            val filteredMessages = messages.filter { item ->
                val hasMessage = item.message.isNotEmpty()
                val hasAttachment = item.attachment != null
                val hasTransaction = item.transactionHash != null
                val messageContainsTerm = item.message.contains(searchTerm.value.toString(), ignoreCase = true)

                fun attachmentTypeOf(type: String): Boolean {
                    return item.attachment!!.type == type
                }

                when (filterType.value.toString()) {
                    FILTER_TYPE_MESSAGE -> hasMessage && !hasAttachment && !hasTransaction && messageContainsTerm
                    FILTER_TYPE_TRANSACTION -> (!hasAttachment && hasTransaction && messageContainsTerm) || (hasAttachment && !hasTransaction && attachmentTypeOf(MessageAttachment.TYPE_TRANSFER_REQUEST) && messageContainsTerm)
                    FILTER_TYPE_PHOTO_VIDEO -> !hasMessage && hasAttachment && !hasTransaction && attachmentTypeOf(MessageAttachment.TYPE_IMAGE)
                    FILTER_TYPE_LOCATION -> hasAttachment && !hasTransaction && attachmentTypeOf(MessageAttachment.TYPE_LOCATION) && messageContainsTerm
                    FILTER_TYPE_CONTACT -> hasAttachment && !hasTransaction && attachmentTypeOf(MessageAttachment.TYPE_CONTACT) && messageContainsTerm
                    FILTER_TYPE_IDENTITY_ATTRIBUTE -> hasAttachment && !hasTransaction && attachmentTypeOf(MessageAttachment.TYPE_IDENTITY_ATTRIBUTE) && messageContainsTerm
                    else -> messageContainsTerm
                }
            }

            totalMessages = filteredMessages.size
            val limitMessages = filteredMessages.takeLast(limitedMessageCount.value?.toInt()!!)

            createMessagesItems(limitMessages)
        }.asLiveData()
    }

    private val peers = MutableLiveData<List<Peer>>(emptyList())

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

    private lateinit var contactName: String
    private var isConnected = false
    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH)

    // Limit the number of shown messages
    private val limitedMessageCount = MutableLiveData(20)
    private var messageCountChanged = false
    private var totalMessages = 0

    // Search and filter the messages
    private var searchTerm = MutableLiveData("")
    private var filterType = MutableLiveData(FILTER_TYPE_EVERYTHING)

    private var cameraUri: Uri? = null

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

        contactName = name

        adapterMessages.registerRenderer(
            ContactChatItemRenderer(
                parentActivity,
                {
                    val attachment = it.chatMessage.attachment
                    val transactionHash = it.chatMessage.transactionHash
                    if (attachment != null) {
                        when (attachment.type) {
                            MessageAttachment.TYPE_IMAGE -> {
                                val file = attachment.getFile(requireContext())
                                if (file.exists()) {
                                    openImage(file)
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
                    messageCountChanged = true
                    limitedMessageCount.value = limitedMessageCount.value?.plus(10)
                },
                getTransactionRepository(),
                blocks
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        parentActivity.toggleActionBar(true)
        parentActivity.setActionBarTitle(contactName, null)
        parentActivity.toggleBottomNavigation(false)

        binding.etMessage.doAfterTextChanged { state ->
            toggleButton(btnSendMessage, state != null && state.isNotEmpty())
        }

        binding.btnSendMessage.setOnClickListener {
            binding.pbSendMessage.isVisible = true
            val message = binding.etMessage.text.toString()
            etMessage.closeKeyboard(requireContext())
            binding.etMessage.text = null
            binding.etMessage.clearFocus()

            Handler().postDelayed({
                getPeerChatCommunity().sendMessage(message, publicKey)
                binding.pbSendMessage.isVisible = false
            }, 500)
        }

        binding.btnNewChatAdd.setOnClickListener {
            addRenameContact(Contact("", publicKey))
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
                    parentActivity.setActionBarTitle(
                        contactName,
                        resources.getString(R.string.text_contact_connected)
                    )
                } else {
                    isConnected = false
                    parentActivity.setActionBarTitle(
                        contactName,
                        null
                    )
                }
            }
        )

        binding.ivAttachment.setOnClickListener {
            OptionsDialog(R.menu.contact_chat_attachments, "Choose Attachment") { _, item ->
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

                                getPeerChatCommunity().sendLocation(location, addressLine, publicKey)
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
            OptionsDialog(R.menu.contact_chat_filter_types) { _, item ->
                filterType.value = item.title.toString()
                binding.ivFilterByType.setImageDrawable(item.icon)
            }.show(parentFragmentManager, tag)
        }

        binding.etSearchMessage.doAfterTextChanged { searchText ->
            ivSearchClearIcon.isVisible = searchText != null && searchText.isNotEmpty()
            searchTerm.value = searchText.toString()
        }

        onFocusChange(binding.etSearchMessage, requireContext())

        itemsMessages.observe(
            viewLifecycleOwner,
            Observer { list ->
                adapterMessages.updateItems(list)
                binding.rvMessages.setItemViewCacheSize(list.size)

                binding.clNewChatRequest.isVisible = getContactStore().getContactFromPublicKey(publicKey) == null && list.none {
                    (it as ContactChatItem).chatMessage.sender == getTrustChainCommunity().myPeer.publicKey
                }

                if (!messageCountChanged) {
                    scrollToBottom(binding.rvMessages)
                }

                messageCountChanged = false
            }
        )

        binding.ivSearchClearIcon.setOnClickListener {
            binding.etSearchMessage.text = null
            searchTerm.value = ""
        }

        val isBlocked = getPeerChatStore().getContactStateForType(publicKey, PeerChatStore.STATUS_BLOCK)
        binding.clBlockedContact.isVisible = isBlocked
        binding.clMessageInputRow.isVisible = !isBlocked

        binding.btnUnblockContact.setOnClickListener {
            toggleBlockContact(
                getPeerChatStore().getContactStateForType(publicKey, PeerChatStore.STATUS_BLOCK)
            )
        }
    }

    override fun onResume() {
        super.onResume()

        parentActivity.apply {
            toggleActionBar(true)
            setActionBarTitle(name, null)
            toggleBottomNavigation(false)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        menu.add(Menu.NONE, MENU_ITEM_SEARCH_FILTER, Menu.NONE, null)
            .setIcon(R.drawable.ic_search_filter)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
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
                if (binding.clSearchFilter.isVisible) {
//                    val slideUpAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.exit_to_top)
//                    binding.clSearchFilter.startAnimation(slideUpAnimation)
                    binding.clSearchFilter.isVisible = false

                    binding.etSearchMessage.text = null
                    binding.ivSearchClearIcon.isVisible = false
                    searchTerm.value = ""
                    if (filterType.value != FILTER_TYPE_EVERYTHING) {
                        filterType.value = FILTER_TYPE_EVERYTHING
                    }
                    binding.ivFilterByType.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.ic_everything
                        )
                    )
                } else {
                    binding.clSearchFilter.isVisible = true
//                    val slideDownAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.enter_from_top)
//                    binding.clSearchFilter.startAnimation(slideDownAnimation)
                }

                toggleSearchFilterBar(!binding.clSearchFilter.isVisible)

                return true
            }
        }

        val menuId = if (getContactStore().getContactFromPublicKey(publicKey) == null) {
            R.menu.contact_chat_options_no_contact
        } else {
            R.menu.contact_chat_options_contact
        }

        var contact = getContactStore().getContactFromPublicKey(publicKey)
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
                                    isShort = false)

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

        binding.clBlockedContact.isVisible = !isBlocked
        binding.clMessageInputRow.isVisible = isBlocked
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
        if ((ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), PERMISSION_CAMERA)

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
//            val themePrefs = parentActivity.getSharedPreferences(ValueTransferMainActivity.preferencesFileName, Context.MODE_PRIVATE).getString(
//                ValueTransferMainActivity.preferencesThemeName,
//                ValueTransferMainActivity.APP_THEME_DAY
//            )
            val currentTheme = appPreferences.getCurrentTheme()
            if (currentTheme == AppPreferences.APP_THEME_NIGHT) {
                color
            } else {
                Color.BLACK
            }
        } else {
            color
        }

        val actionBarTypedValue = TypedValue()
        parentActivity.theme.resolveAttribute(
            R.attr.onBackgroundColor,
            actionBarTypedValue,
            true
        )

        val actionBarTitleColor = if (hide) {
            ContextCompat.getColor(requireContext(), actionBarTypedValue.resourceId)
        } else Color.WHITE

        parentActivity.supportActionBar!!.apply {
            setBackgroundDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    typedValue.resourceId
                )
            )
            customView.apply {
                findViewById<TextView>(R.id.tv_actionbar_title).setTextColor(actionBarTitleColor)
                findViewById<TextView>(R.id.tv_actionbar_subtitle).setTextColor(actionBarTitleColor)
            }
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

        previousFragment[0].onResume()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE -> if (data != null) {
                    data.data?.let { uri ->
                        sendFromUri(uri, TYPE_IMAGE)
                    }
                }
                PICK_CAMERA -> cameraUri?.let {
                    sendFromUri(it, TYPE_IMAGE)
                }
                RENAME_CONTACT -> if (data != null) {
                    contactName = data.data.toString()
                    parentActivity.setActionBarTitle(
                        contactName,
                        if (isConnected) resources.getString(R.string.text_contact_connected) else null
                    )
                    searchTerm.value = searchTerm.value
                }
            }
        } else super.onActivityResult(requestCode, resultCode, data)
    }

    private fun sendFromUri(uri: Uri, type: String) {
        val file = saveFile(requireContext(), uri)

        when (type) {
            TYPE_IMAGE -> getPeerChatCommunity().sendImage(file, publicKey)
        }
    }

    private fun createMessagesItems(messages: List<ChatMessage>): List<Item> {
        return messages.mapIndexed { index, chatMessage ->
            ContactChatItem(
                chatMessage,
                getTransactionRepository().getTransactionWithHash(chatMessage.transactionHash),
                (index == 0) && (totalMessages >= limitedMessageCount.value?.toInt()!!),
                (index == 0) || (index > 0 && !dateFormat.format(messages[index-1].timestamp).equals(dateFormat.format(chatMessage.timestamp))),
                false
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
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED}) {
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
        const val PICK_CAMERA = 22
        const val RENAME_CONTACT = 33

        const val MENU_ITEM_OPTIONS = 1
        const val MENU_ITEM_SEARCH_FILTER = 2

        const val TYPE_IMAGE = "image"

        const val FILTER_TYPE_EVERYTHING = "Everything"
        const val FILTER_TYPE_MESSAGE = "Message"
        const val FILTER_TYPE_TRANSACTION = "Transaction"
        const val FILTER_TYPE_PHOTO_VIDEO = "Photo/Video"
        const val FILTER_TYPE_LOCATION = "Location"
        const val FILTER_TYPE_CONTACT = "Contact"
        const val FILTER_TYPE_IDENTITY_ATTRIBUTE = "Identity Attribute"

        const val PERMISSION_LOCATION = 1
        const val PERMISSION_CAMERA = 2
    }
}
