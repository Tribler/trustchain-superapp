package nl.tudelft.trustchain.valuetransfer.ui.contacts

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.appcompat.widget.PopupMenu
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.contacts.ContactStore
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
import nl.tudelft.trustchain.valuetransfer.dialogs.*
import nl.tudelft.trustchain.valuetransfer.entity.IdentityAttribute
import nl.tudelft.trustchain.valuetransfer.util.*
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ContactChatFragment : BaseFragment(R.layout.fragment_contacts_chat) {
    private val binding by viewBinding(FragmentContactsChatBinding::bind)
    private lateinit var parentActivity: ValueTransferMainActivity
    private lateinit var peerChatCommunity: PeerChatCommunity
    private lateinit var peerChatStore: PeerChatStore
    private lateinit var contactStore: ContactStore
    private lateinit var transactionRepository: TransactionRepository

    private val adapterMessages = ItemAdapter()

    private val itemsMessages: LiveData<List<Item>> by lazy {
        combine(peerChatStore.getAllByPublicKey(publicKey), limitedMessageCount.asFlow(), searchTerm.asFlow(), filterType.asFlow()) { messages, _, _, _ ->

            val filteredMessages = messages.filter { item ->
                val hasMessage = item.message.isNotEmpty()
                val hasAttachment = item.attachment != null
                val hasTransaction = item.transactionHash != null
                val messageContainsTerm = item.message.contains(searchTerm.value.toString(), ignoreCase = true)

                fun attachmentTypeOf(type: String): Boolean {
                    return item.attachment!!.type == type
                }

                when(filterType.value.toString()) {
                    FILTER_TYPE_MESSAGE -> hasMessage && !hasAttachment && !hasTransaction && messageContainsTerm
                    FILTER_TYPE_TRANSACTION -> (!hasAttachment && hasTransaction && messageContainsTerm) || (hasAttachment && !hasTransaction && attachmentTypeOf(MessageAttachment.TYPE_TRANSFER_REQUEST) && messageContainsTerm)
                    FILTER_TYPE_PHOTO_VIDEO -> !hasMessage && hasAttachment && !hasTransaction && attachmentTypeOf(MessageAttachment.TYPE_IMAGE)
                    FILTER_TYPE_FILE -> hasAttachment && !hasTransaction && attachmentTypeOf(MessageAttachment.TYPE_FILE) && messageContainsTerm
                    FILTER_TYPE_LOCATION -> !hasMessage && hasAttachment && !hasTransaction && attachmentTypeOf(MessageAttachment.TYPE_LOCATION)
                    FILTER_TYPE_CONTACT -> !hasMessage && hasAttachment && !hasTransaction && attachmentTypeOf(MessageAttachment.TYPE_CONTACT)
                    FILTER_TYPE_IDENTITY_ATTRIBUTE -> !hasMessage && hasAttachment && !hasTransaction && attachmentTypeOf(MessageAttachment.TYPE_IDENTITY_ATTRIBUTE)
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

    private val dateFormat = SimpleDateFormat("dd-MM-yyyy")

    // Limit the number of shown messages
    private val limitedMessageCount = MutableLiveData(20)
    private var messageCountChanged = false
    private var totalMessages = 0

    // Search and filter the messages
    private var searchTerm = MutableLiveData("")
    private var filterType = MutableLiveData(FILTER_TYPE_EVERYTHING)

    // Remember file and filename for sending
    private lateinit var selectedFile: File
    private lateinit var originalFileName: String

    init {
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_contacts_chat, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        parentActivity = requireActivity() as ValueTransferMainActivity
        peerChatCommunity = parentActivity.getCommunity(ValueTransferMainActivity.peerChatCommunityTag) as PeerChatCommunity
        peerChatStore = parentActivity.getStore(ValueTransferMainActivity.peerChatStoreTag) as PeerChatStore
        contactStore = parentActivity.getStore(ValueTransferMainActivity.contactStoreTag) as ContactStore
        transactionRepository = parentActivity.getStore(ValueTransferMainActivity.transactionRepositoryTag) as TransactionRepository

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
                                    imageDialog(file)
                                } else {
                                    parentActivity.displaySnackbar(requireContext(), "File does not exists (yet)", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR)
                                }
                            }
                            MessageAttachment.TYPE_FILE -> {
                                selectedFile = attachment.getFile(requireContext())
                                originalFileName = it.chatMessage.message

                                val extension = originalFileName.split(".")[1]

                                val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                                Log.d("VTLOG", "EXTENSION: $extension $mime")

                                parentActivity.displaySnackbar(requireContext(), "Save File (TODO)", type = ValueTransferMainActivity.SNACKBAR_TYPE_WARNING)
                            }
                            MessageAttachment.TYPE_CONTACT -> {
                                val contact = Contact.deserialize(attachment.content, 0).first

                                if(contactStore.getContactFromPublicKey(contact.publicKey) == null) {
                                    when (contact.publicKey) {
                                        getTrustChainCommunity().myPeer.publicKey -> parentActivity.displaySnackbar(requireContext(), "You can't add yourself as contact", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR)
                                        else -> {
                                            ConfirmDialog("Do you want to add contact ${contact.name} to your address book?") { dialog ->
                                                try {
                                                    contactStore.addContact(contact.publicKey, contact.name)
                                                    parentActivity.displaySnackbar(requireContext(), "Contact ${contact.name} added to address book")

                                                    dialog.dismiss()

                                                }catch(e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                                .show(parentFragmentManager, tag)
                                        }
                                    }
                                }else{
                                    goToContactFragment(contact)
                                }
                            }
                            MessageAttachment.TYPE_LOCATION -> {
                                val offsetBuffer = attachment.content.copyOfRange(0, attachment.content.size)
                                JSONObject(offsetBuffer.decodeToString()).let { json ->
                                    val latitude = json.getString("latitude")
                                    val longitude = json.getString("longitude")

                                    val browserIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.google.com/maps/place/$latitude,$longitude")
                                    )
                                    startActivity(browserIntent)
                                }
                            }
                            MessageAttachment.TYPE_IDENTITY_ATTRIBUTE -> {
                                val attributeValue = IdentityAttribute.deserialize(it.chatMessage.attachment!!.content, 0).first
                                copyToClipboard(requireContext(), attributeValue.value, "Identity attribute")
                                parentActivity.displaySnackbar(requireContext(), "Identity attribute value has been copied to clipboard")
                            }
                            MessageAttachment.TYPE_TRANSFER_REQUEST -> {
                                val offsetBuffer = attachment.content.copyOfRange(0, attachment.content.size)
                                JSONObject(offsetBuffer.decodeToString()).let { json ->
                                    val description = if(it.chatMessage.message.isNotEmpty()) "Transfer of request: ${it.chatMessage.message}" else null

                                    if(json.has("amount")) {
                                        val amount = json.getString("amount")
                                        var contact = contactStore.getContactFromPublicKey(publicKey)

                                        if(contact != null) {
                                            ExchangeTransferMoneyDialog(contact, amount, true, description).show(parentFragmentManager, tag)
                                        }else{
                                            parentActivity.displaySnackbar(requireContext(), "Please add contact first to transfer money", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if(transactionHash != null) {
                        val transaction = transactionRepository.getTransactionWithHash(transactionHash)
                        val key = defaultCryptoProvider.keyFromPublicBin(transaction!!.linkPublicKey)
                        val peer = Peer(key)
                        transactionRepository.trustChainCommunity.sendBlock(transaction, peer)
                    }
                }, {
                    messageCountChanged = true
                    limitedMessageCount.value = limitedMessageCount.value?.plus(10)
                }
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        parentActivity.toggleActionBar(true)
        parentActivity.setActionBarTitle(name)
        parentActivity.toggleBottomNavigation(false)

        etMessage.doAfterTextChanged { state ->
            toggleButton(btnSendMessage, state != null && state.isNotEmpty())
        }

        btnSendMessage.setOnClickListener {
            peerChatCommunity.sendMessage(binding.etMessage.text.toString(), publicKey)
            binding.etMessage.text = null
            binding.etMessage.clearFocus()
            closeKeyboard(requireContext(), etMessage)
        }

        lifecycleScope.launchWhenResumed {
            while (isActive) {
                peers.value = peerChatCommunity.getPeers()
                delay(1000L)
            }
        }

        peers.observe(
            viewLifecycleOwner,
            Observer { list ->
                val peer = list.find { it.mid == publicKey.keyToHash().toHex() }

                if((peer != null && !peer.address.isEmpty()) || (peer?.bluetoothAddress != null)) {
                    parentActivity.setActionBarTitle("Connected with IPv8", false)
                }else{
                    parentActivity.setActionBarTitle("", false)
                }
            }
        )

        val optionsMenuButton = binding.ivAttachment
        optionsMenuButton.setOnClickListener {
            val optionsMenu = PopupMenu(requireContext(), optionsMenuButton)
            optionsMenu.menuInflater.inflate(R.menu.contact_chat_attachments, optionsMenu.menu)
            optionsMenu.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item ->
                val contact = contactStore.getContactFromPublicKey(publicKey)

                when(item.itemId) {
                    R.id.actionSendPhotoVideo -> {
                        val mimeTypes = arrayOf("image/*", "video/*")
                        val intent = Intent(Intent.ACTION_GET_CONTENT)
                        intent.type = "*/*"
                        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                        startActivityForResult(Intent.createChooser(intent, "Send Photo or Video"), PICK_IMAGE)
                    }
                    R.id.actionSendFile -> {
                        val mimeTypes = arrayOf("application/*", "text/plain")
                        val intent = Intent(Intent.ACTION_GET_CONTENT)
                        intent.type = "*/*"
                        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                        startActivityForResult(Intent.createChooser(intent, "Send File"), PICK_FILE)
                    }
                    R.id.actionSendLocation -> {
                        getLocation()?.let { location ->
                            val geocoder = Geocoder(requireContext(), Locale.getDefault())
                            val address = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                            var addressLine = "No address found using these coordinates: ${location.latitude} ${location.longitude}"

                            try {
                                addressLine = address[0].getAddressLine(0)
                            }catch (e: Exception) {
                                e.printStackTrace()
                            }

                            peerChatCommunity.sendLocation(location, addressLine, publicKey)
                            return@OnMenuItemClickListener true
                        }
                        parentActivity.displaySnackbar(requireContext(), "The location could not be retrieved, please grant permissions or try again", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR)
                    }
                    R.id.actionSendContact -> ContactShareDialog(null, contact).show(parentFragmentManager, tag)
                    R.id.actionSendIdentityAttribute -> IdentityAttributeShareDialog(contact, null).show(parentFragmentManager, tag)
                    R.id.actionTransferMoney -> {
                        if(contact != null) {
                            ExchangeTransferMoneyDialog(contact, null, true).show(parentFragmentManager, tag)
                        } else {
                            parentActivity.displaySnackbar(requireContext(), "Please add contact first to transfer money", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR)
                        }
                    }
                    R.id.actionRequestMoney -> {
                        if(contact != null) {
                            ExchangeTransferMoneyDialog(contact, null, false).show(parentFragmentManager, tag)
                        } else {
                            parentActivity.displaySnackbar(requireContext(), "Please add contact first to request money", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR)
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

        val filterAdapter = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, FILTER_TYPES) {
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val textView: TextView = super.getDropDownView(position, convertView, parent) as TextView
                val params = textView.layoutParams
                params.height = resources.getDimensionPixelSize(R.dimen.textViewHeight)
                textView.layoutParams = params
                textView.gravity = Gravity.CENTER_VERTICAL

                textView.textSize = 14F
                textView.text = FILTER_TYPES[position]

                if(position == 0) {
                    textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_gray))
                    textView.setTypeface(null, Typeface.ITALIC)
                }

                if(position == spinnerFilter.selectedItemPosition) {
                    textView.background = ColorDrawable(Color.LTGRAY)
                }

                return textView
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val imageView = ImageView(requireContext())
                imageView.setBackgroundResource(FILTER_TYPES_IMAGES[position])
                imageView.background.setTint(ContextCompat.getColor(requireContext(), R.color.gray))
                return imageView
            }
        }

        binding.spinnerFilter.adapter = filterAdapter

        binding.spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                filterType.value = FILTER_TYPES[position]
            }
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

                if(!messageCountChanged) {
                    scrollToBottom(binding.rvMessages)
                }

                messageCountChanged = false
            }
        )

        binding.ivSearchClearIcon.setOnClickListener {
            binding.etSearchMessage.setText("")
            searchTerm.value = ""
        }
    }

    override fun onResume() {
        super.onResume()

        parentActivity.toggleActionBar(true)
        parentActivity.setActionBarTitle(name)
        parentActivity.toggleBottomNavigation(false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.contact_chat_options, menu)

        if(contactStore.getContactFromPublicKey(publicKey) == null) {
            menu.getItem(0).title = "Add contact"
            menu.getItem(1).title = "Remove local conversation"
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var contact = contactStore.getContactFromPublicKey(publicKey)

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
                    ConfirmDialog("Are u sure to remove the conversation completely?") { dialog ->
                        try {
                            for(chatItem in adapterMessages.items) {
                                peerChatStore.deleteMessage(chatItem as ChatMessageItem)
                            }
                            parentActivity.displaySnackbar(requireContext(), "Local conversation has been removed", isShort = false)
                            dialog.dismiss()

                            onBackPressed()
                        }catch(e: Exception) {
                            e.printStackTrace()
                        }
                    }
                        .show(parentFragmentManager, tag)
                }else{
                    ConfirmDialog("Are u sure you want to remove ${contact.name}?") { dialog ->
                        try {
                            contactStore.deleteContact(contact)
                            parentActivity.displaySnackbar(requireContext(), "${contact.name} removed from address book", isShort = false)
                            dialog.dismiss()

                            onBackPressed()
                        }catch(e: Exception) {
                            e.printStackTrace()
                        }
                    }
                        .show(parentFragmentManager, tag)
                }
            }
            R.id.actionCopyPublicKey -> {
                copyToClipboard(requireContext(), (contact?.publicKey ?: publicKey).keyToBin().toHex(), "Public key")
                parentActivity.displaySnackbar(requireContext(), "Public key has been copied to clipboard")
            }
            R.id.actionShareContact -> ContactShareDialog(contact, null).show(parentFragmentManager, tag)
            R.id.actionSearchFilterChat -> {
                if(binding.clSearchFilter.isVisible) {
                    binding.clSearchFilter.isVisible = false
                    val slideUpAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)
                    binding.clSearchFilter.startAnimation(slideUpAnimation)
                    binding.etSearchMessage.setText("")
                    binding.ivSearchClearIcon.isVisible = false
                    parentActivity.window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.colorPrimaryValueTransfer)
                    parentActivity.supportActionBar!!.setBackgroundDrawable(ContextCompat.getDrawable(requireActivity(),R.color.colorPrimaryValueTransfer))
                    searchTerm.value = ""
                    filterType.value = FILTER_TYPE_EVERYTHING
                    spinnerFilter.setSelection(0)
                }else{
                    binding.clSearchFilter.isVisible = true
                    val slideDownAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_down)
                    binding.clSearchFilter.startAnimation(slideDownAnimation)
                    parentActivity.window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.colorYellow)
                    parentActivity.supportActionBar!!.setBackgroundDrawable(ContextCompat.getDrawable(requireActivity(),R.color.colorYellow))
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getLocation(): Location? {
        if ((ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
            (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION)
        }else{
            val locationManager = parentActivity.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            for(provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
                locationManager.getLastKnownLocation(provider)?.let { location ->
                    return location
                }
            }
        }
        return null
    }

    private fun imageDialog(file: File) {
        val dialog = Dialog(requireContext())
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(layoutInflater.inflate(R.layout.dialog_image, null))
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        dialog.ivImageFullScreen.setImageBitmap(bitmap)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.show()
    }

    private fun hideSearchFilterBar() {
        binding.clSearchFilter.isVisible = false
        parentActivity.window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.colorPrimaryValueTransfer)
        parentActivity.supportActionBar!!.setBackgroundDrawable(ContextCompat.getDrawable(requireActivity(),R.color.colorPrimaryValueTransfer))
    }

    private fun goToContactFragment(contact: Contact) {
        val args = Bundle()
        args.putString(ValueTransferMainActivity.ARG_PUBLIC_KEY, contact.publicKey.keyToBin().toHex())
        args.putString(ValueTransferMainActivity.ARG_NAME, contact.name)
        args.putString(ValueTransferMainActivity.ARG_PARENT, ValueTransferMainActivity.walletOverviewFragmentTag)

        val contactChatFragment = ContactChatFragment()
        contactChatFragment.arguments = args

        hideSearchFilterBar()

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(0, R.anim.exit_to_left)
            .remove(this)
            .setCustomAnimations(R.anim.enter_from_right, 0)
            .add(R.id.container, contactChatFragment,
                ValueTransferMainActivity.contactChatFragmentTag
            )
            .commit()
    }

    private fun onBackPressed() {
        hideSearchFilterBar()
        parentActivity.setActionBarTitle("",false)
        closeKeyboard(requireContext(), etMessage)

        val previousFragment = parentFragmentManager.fragments.filter {
            it.tag == parentTag
        }

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(0, R.anim.exit_to_right)
            .remove(this)
            .setCustomAnimations(R.anim.enter_from_left, 0)
            .show(previousFragment[0])
            .commit()
        previousFragment[0].onResume()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            PICK_IMAGE -> if (resultCode == Activity.RESULT_OK && data != null) {
                val uri = data.data
                if (uri != null) {
                    sendFromUri(uri, TYPE_IMAGE_VIDEO)
                }
            }
            PICK_FILE -> if(resultCode == Activity.RESULT_OK && data != null) {
                val uri = data.data
                if(uri != null) {
                    val fileName = if(uri.path != null) {
                        File(uri.path!!).name
                    }else{
                        "unknown"
                    }
                    sendFromUri(uri, TYPE_FILE, fileName)
                }
            }
            RENAME_CONTACT -> if(resultCode == Activity.RESULT_OK && data != null)  {
                val newName = data.data.toString()
                parentActivity.setActionBarTitle(newName)
            }
            DIRECTORY_CHOOSER -> if(resultCode == Activity.RESULT_OK && data != null){
                data.data?.let { treeUri ->
                    Log.d("VTLOG", "SELECTED TREE URI: ${treeUri.path}")
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun sendFromUri(uri: Uri, type: String, text: String? = "") {
        val file = saveFile(requireContext(), uri)

        when(type) {
            TYPE_IMAGE_VIDEO -> peerChatCommunity.sendImage(file, publicKey)
            TYPE_FILE -> peerChatCommunity.sendFile(file, text!!, publicKey)
        }
    }

    private fun createMessagesItems(messages: List<ChatMessage>): List<Item> {
        return messages.mapIndexed { index, chatMessage ->
            ChatMessageItem(
                chatMessage,
                transactionRepository.getTransactionWithHash(chatMessage.transactionHash),
                (index == 0) && (totalMessages >= limitedMessageCount.value?.toInt()!!),
                (index == 0) || (index > 0 && !dateFormat.format(messages[index-1].timestamp).equals(dateFormat.format(chatMessage.timestamp))),
                name
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                parentActivity.displaySnackbar(requireContext(), "Permission has been granted")
            } else {
                parentActivity.displaySnackbar(requireContext(), "Permission has been denied", type = ValueTransferMainActivity.SNACKBAR_TYPE_ERROR)
            }
        }
    }

    companion object {
        const val PICK_IMAGE = 1
        const val PICK_FILE = 2
        const val DIRECTORY_CHOOSER = 4

        const val RENAME_CONTACT = 10

        const val TYPE_IMAGE_VIDEO = "image_video"
        const val TYPE_FILE = "file"

        const val FILTER_TYPE_EVERYTHING = "Everything"
        const val FILTER_TYPE_MESSAGE = "Message"
        const val FILTER_TYPE_TRANSACTION = "Transaction"
        const val FILTER_TYPE_PHOTO_VIDEO = "Photo/Video"
        const val FILTER_TYPE_FILE = "File"
        const val FILTER_TYPE_LOCATION = "Location"
        const val FILTER_TYPE_CONTACT = "Contact"
        const val FILTER_TYPE_IDENTITY_ATTRIBUTE = "Identity Attribute"

        val FILTER_TYPES = listOf(
            FILTER_TYPE_EVERYTHING,
            FILTER_TYPE_MESSAGE,
            FILTER_TYPE_TRANSACTION,
            FILTER_TYPE_PHOTO_VIDEO,
            FILTER_TYPE_FILE,
            FILTER_TYPE_LOCATION,
            FILTER_TYPE_CONTACT,
            FILTER_TYPE_IDENTITY_ATTRIBUTE,
        )

        val FILTER_TYPES_IMAGES = listOf(
            R.drawable.ic_baseline_border_all_24,
            R.drawable.ic_baseline_message_24,
            R.drawable.ic_exchange,
            R.drawable.ic_camera_alt_black_24dp,
            R.drawable.ic_baseline_attach_file_24,
            R.drawable.ic_baseline_location_on_24,
            R.drawable.ic_baseline_person_24,
            R.drawable.ic_attribute,
        )

        const val LOCATION_PERMISSION = 1
    }
}
