package nl.tudelft.trustchain.valuetransfer.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mattskala.itemadapter.ItemAdapter
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
import android.graphics.BitmapFactory
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import kotlinx.coroutines.flow.combine
import nl.tudelft.trustchain.valuetransfer.util.*

class ChatMediaDialog(
    private val c: Context,
    private val publicKey: PublicKey,
    private val chatMediaItem: ChatMediaItem? = null,
) : VTDialogFragment(), View.OnClickListener, Toolbar.OnMenuItemClickListener {
    private var initialLaunch = true
    private lateinit var bottomSheetDialog: Dialog
    private lateinit var dialogView: View

    private var filterType = MutableLiveData(FILTER_TYPE_ALL)
    private var isFiltering = false

    private val itemsChatMedia: LiveData<List<ChatMediaItem>> by lazy {
        combine(getPeerChatStore().getAllByPublicKey(publicKey), filterType.asFlow()) { messages, type ->
            val filterTypes = when (type) {
                FILTER_TYPE_IMAGES -> listOf(MessageAttachment.TYPE_IMAGE)
                FILTER_TYPE_FILES -> listOf(MessageAttachment.TYPE_FILE)
                else -> listOf(MessageAttachment.TYPE_IMAGE, MessageAttachment.TYPE_FILE)
            }

            val items = messages.filter {
                filterTypes.contains(it.attachment?.type) && it.attachment?.getFile(requireContext()) != null
            }.asReversed()
            createMediaItems(items)
        }.asLiveData()
    }

    private lateinit var rvChatMediaItems: RecyclerView
    private val adapterChatMedia = ItemAdapter()
    private var viewPager: ViewPager? = null
    private val chatMediaDetailAdapter: ChatMediaDetailAdapter = ChatMediaDetailAdapter(c) {
        galleryView(it)
    }

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

            @Suppress("DEPRECATION")
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
            viewPager!!.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageSelected(p0: Int) {
                    chatMediaDetailAdapter.getItem(p0).let { item ->
                        senderTitle.text = item.senderName
                        dateTitle.text = dateFormat.format(item.sendDate)
                    }
                }

                override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {}
                override fun onPageScrollStateChanged(p0: Int) {}
            })

            // Recyclerview and adapter for chat image gallery
            adapterChatMedia.registerRenderer(
                ChatMediaItemRenderer {
                    senderTitle.text = it.senderName
                    dateTitle.text = dateFormat.format(it.sendDate)

                    chatMediaDetailAdapter.notifyDataSetChanged()
                    viewPager!!.setCurrentItem(chatMediaDetailAdapter.getIndexOf(it), false)
                    galleryView(false)
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

                    chatMediaDetailAdapter.setItems(list, isFiltering)
                    viewPager!!.invalidate()

                    if (isFiltering) {
                        isFiltering = false
                        return@Observer
                    }

                    if (initialLaunch) {
                        if (chatMediaItem == null) {
                            galleryView(true)
                        } else {
                            val index = chatMediaDetailAdapter.getIndexOf(chatMediaItem)
                            viewPager!!.setCurrentItem(index, false)
                            senderTitle.text = chatMediaDetailAdapter.getItem(index).senderName
                            dateTitle.text = dateFormat.format(chatMediaDetailAdapter.getItem(index).sendDate)
                            galleryView(false)
                        }
                        initialLaunch = false
                    }

                    if (galleryView.isVisible) {
                        senderTitle.text = resources.getString(R.string.text_all_media)
                        dateTitle.text = resources.getString(R.string.text_number_media, list.size)
                    }
                }
            )

            val showAll = view.findViewById<TextView>(R.id.tvShowAll)
            val showImages = view.findViewById<TextView>(R.id.tvShowImages)
            val showFiles = view.findViewById<TextView>(R.id.tvShowFiles)

            showAll.setOnClickListener {
                (it as TextView).filterMedia(FILTER_TYPE_ALL, listOf(showImages, showFiles))
            }
            showImages.setOnClickListener {
                (it as TextView).filterMedia(FILTER_TYPE_IMAGES, listOf(showAll, showFiles))
            }
            showFiles.setOnClickListener {
                (it as TextView).filterMedia(FILTER_TYPE_FILES, listOf(showAll, showImages))
            }

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
                val currentItem = chatMediaDetailAdapter.getItem(index)

                if (!storageIsWritable()) {
                    parentActivity.displayToast(c, resources.getString(R.string.text_storage_not_writable))
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
                senderTitle.text = resources.getString(R.string.text_all_media)
                dateTitle.text = resources.getString(R.string.text_number_media, adapterChatMedia.itemCount)
                galleryView(true)
            }
        }
        return true
    }

    private fun galleryView(isGallery: Boolean) {
        galleryView.isVisible = isGallery
        detailView.isVisible = !isGallery

        actionBar.menu.getItem(0).isVisible = !isGallery
        actionBar.menu.getItem(1).isVisible = !isGallery
    }

    private fun TextView.filterMedia(type: String, others: List<TextView>) {
        if (filterType.value == type) return
        isFiltering = true
        filterType.postValue(type)
        this.apply {
            setTypeface(null, Typeface.BOLD)
            background =
                ContextCompat.getDrawable(requireContext(), R.drawable.pill_rounded_selected)
        }

        others.forEach {
            it.apply {
                setTypeface(null, Typeface.NORMAL)
                background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.pill_rounded)
            }
        }
    }

    private fun createMediaItems(messages: List<ChatMessage>): List<ChatMediaItem> {
        val myKey = getTrustChainCommunity().myPeer.publicKey
        return messages.map { item ->
            val senderName = if (myKey == item.sender) {
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

    companion object {
        const val FILTER_TYPE_ALL = "filter_all"
        const val FILTER_TYPE_IMAGES = "filter_images"
        const val FILTER_TYPE_FILES = "filter_files"
    }
}
