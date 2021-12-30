package nl.tudelft.trustchain.valuetransfer.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mattskala.itemadapter.Item
import com.mattskala.itemadapter.ItemAdapter
import kotlinx.coroutines.flow.map
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.trustchain.peerchat.entity.ChatMessage
import nl.tudelft.trustchain.peerchat.ui.conversation.MessageAttachment
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ui.VTDialogFragment
import java.text.SimpleDateFormat
import java.util.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.viewpager.widget.ViewPager
import nl.tudelft.trustchain.valuetransfer.ui.contacts.ChatMediaDetailAdapter
import nl.tudelft.trustchain.valuetransfer.ui.contacts.ChatMediaItem
import nl.tudelft.trustchain.valuetransfer.ui.contacts.ChatMediaItemRenderer
import android.widget.Toast

import android.graphics.BitmapFactory
import nl.tudelft.trustchain.valuetransfer.util.*

class ChatMediaDialog(
    private val c: Context,
    private val publicKey: PublicKey,
    private val chatMediaItem: ChatMediaItem? = null,
) : VTDialogFragment(), View.OnClickListener, Toolbar.OnMenuItemClickListener {
    private var initialLaunch = true
    private lateinit var bottomSheetDialog: Dialog
    private lateinit var dialogView: View

    private val itemsChatMedia: LiveData<List<Item>> by lazy {
        getPeerChatStore().getAllByPublicKey(publicKey).map { messages ->
            val items = messages.filter {
                listOf(MessageAttachment.TYPE_IMAGE, MessageAttachment.TYPE_FILE).contains(it.attachment?.type) && it.attachment?.getFile(requireContext()) != null
            }.asReversed()
            createMediaItems(items)
        }.asLiveData()
    }

    private lateinit var rvChatMediaItems: RecyclerView
    private val adapterChatMedia = ItemAdapter()
    private var viewPager: ViewPager? = null
    private val chatMediaDetailAdapter: ChatMediaDetailAdapter = ChatMediaDetailAdapter(c,
        {
            initView(false, it as ChatMediaItem)
        }, {
            initView(true, null)
        }
    )

    private lateinit var actionBar: Toolbar
    private lateinit var galleryView: LinearLayout
    private lateinit var detailView: LinearLayout
    private lateinit var noMediaText: TextView
    private lateinit var senderTitle: AppCompatTextView
    private lateinit var dateTitle: AppCompatTextView

    private val dateFormat = SimpleDateFormat("dd-MM-yyyy, HH:mm", Locale.ENGLISH)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            bottomSheetDialog = Dialog(requireContext(), R.style.FullscreenDialog)
            bottomSheetDialog.window?.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

            val view = layoutInflater.inflate(R.layout.dialog_contact_chat_media, null)
            dialogView = view

            // Dialog cannot be discarded on outside touch
            bottomSheetDialog.setCancelable(false)
            bottomSheetDialog.setCanceledOnTouchOutside(false)

            // Set action bar
            setHasOptionsMenu(true)
            actionBar = view.findViewById(R.id.tbActionBar)
            actionBar.inflateMenu(R.menu.contact_chat_media)
            actionBar.setOnMenuItemClickListener(this)
            actionBar.setNavigationOnClickListener {
                bottomSheetDialog.dismiss()
            }

            actionBar.menu.getItem(0).isVisible = chatMediaItem != null

            // Gallery and detail view and action bar titles
            galleryView = view.findViewById(R.id.clImageGallery)
            detailView = view.findViewById(R.id.clImageDetail)
            noMediaText = view.findViewById(R.id.tvNoMedia)
            senderTitle = view.findViewById(R.id.tvActionbarSender)
            dateTitle = view.findViewById(R.id.tvActionbarTime)

            // Viewpager and custom adapter for the detail display
            viewPager = view.findViewById(R.id.viewPager) as ViewPager
            viewPager!!.adapter = chatMediaDetailAdapter

            // Recyclerview and adapter for chat image gallery
            adapterChatMedia.registerRenderer(
                ChatMediaItemRenderer {
                    viewPager!!.setCurrentItem(chatMediaDetailAdapter.getIndexOf(it), false)
                    initView(false, it)
                }
            )

            rvChatMediaItems = view.findViewById(R.id.rvChatMediaItems)
            rvChatMediaItems.adapter = adapterChatMedia
            rvChatMediaItems.layoutManager = GridLayoutManager(requireContext(), 3)

            itemsChatMedia.observe(
                this,
                Observer { list ->
                    adapterChatMedia.updateItems(list)
                    rvChatMediaItems.setItemViewCacheSize(list.size)

                    noMediaText.isVisible = list.isEmpty()

                    chatMediaDetailAdapter.setItems(list)
                    viewPager!!.invalidate()

                    if (initialLaunch) {
                        if (chatMediaItem == null) {
                            initView(true, null)
                        } else {
                            viewPager!!.setCurrentItem(chatMediaDetailAdapter.getIndexOf(chatMediaItem), false)
                            initView(false, chatMediaItem)
                        }
                        initialLaunch = false
                    }

                    if (galleryView.isVisible) {
                        senderTitle.text = resources.getString(R.string.text_all_media)
                        dateTitle.text = resources.getString(R.string.text_number_media, list.size)
                    }
                }
            )

            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()

            bottomSheetDialog
        } ?: throw IllegalStateException(resources.getString(R.string.text_activity_not_null_requirement))
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            android.R.id.home -> bottomSheetDialog.dismiss()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.actionSaveFile -> {
                val index = viewPager!!.currentItem
                val currentItem = chatMediaDetailAdapter.getItem(index) as ChatMediaItem

                if (!storageIsWritable()) {
                    Toast.makeText(c, resources.getString(R.string.text_storage_not_writable), Toast.LENGTH_SHORT).show()
                    return true
                }

                when (currentItem.type) {
                    MessageAttachment.TYPE_IMAGE -> BitmapFactory.decodeFile(currentItem.file.path).let { bitmap ->
                            saveImage(c, bitmap, currentItem.file.name)
                        }
                    MessageAttachment.TYPE_FILE -> saveFile(c, currentItem.file, currentItem.fileName ?: currentItem.file.name)
                }
            }
            R.id.actionAllMedia -> {
                initView(true, null)
            }
        }
        return true
    }

    private fun initView(isGallery: Boolean = true, item: ChatMediaItem? = null) {
        senderTitle.text = item?.senderName ?: resources.getString(R.string.text_all_media)
        dateTitle.text = if (item == null) {
            resources.getString(R.string.text_number_media, adapterChatMedia.itemCount)
        } else dateFormat.format(item.sendDate)

        galleryView.isVisible = isGallery
        detailView.isVisible = !isGallery

        actionBar.menu.getItem(0).isVisible = !isGallery
        actionBar.menu.getItem(1).isVisible = !isGallery
    }

    private fun createMediaItems(messages: List<ChatMessage>): List<Item> {
        return messages.map { item ->
            val senderName = if (getTrustChainCommunity().myPeer.publicKey == item.sender) {
                resources.getString(R.string.text_you)
            } else getContactStore().getContactFromPublicKey(publicKey)?.name ?: resources.getString(R.string.text_unknown_contact)

            val messageID = item.id
            val attachmentFile = item.attachment!!.getFile(requireContext())
            val type = item.attachment!!.type
            val fileName = item.message

            ChatMediaItem(
                messageID,
                senderName,
                item.timestamp,
                type,
                attachmentFile,
                fileName
            )
        }
    }
}






