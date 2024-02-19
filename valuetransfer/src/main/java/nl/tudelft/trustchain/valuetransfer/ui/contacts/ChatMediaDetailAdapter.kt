package nl.tudelft.trustchain.valuetransfer.ui.contacts

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.jsibbold.zoomage.ZoomageView
import nl.tudelft.trustchain.valuetransfer.databinding.ItemContactChatMediaDetailDocumentBinding
import nl.tudelft.trustchain.valuetransfer.databinding.ItemContactChatMediaDetailImageBinding
import nl.tudelft.trustchain.valuetransfer.util.MessageAttachment
import nl.tudelft.trustchain.valuetransfer.util.OnSwipeTouchListener
import nl.tudelft.trustchain.valuetransfer.util.getFormattedSize

@SuppressLint("ClickableViewAccessibility")
class ChatMediaDetailAdapter(
    context: Context,
    private val onItem: (Boolean) -> Unit
) : PagerAdapter() {
    var layoutInflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private var items: List<ChatMediaItem> = listOf()

    fun setItems(
        items: List<ChatMediaItem>,
        silent: Boolean = false
    ) {
        this.items = items
        if (!silent) notifyDataSetChanged()
    }

    override fun isViewFromObject(
        view: View,
        `object`: Any
    ): Boolean {
        return view === `object` as LinearLayout
    }

    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
    }

    fun getIndexOf(item: ChatMediaItem): Int {
        return items.indexOfFirst {
            it.areItemsTheSame(item)
        }
    }

    fun getItem(index: Int): ChatMediaItem {
        return items[index]
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun instantiateItem(
        container: ViewGroup,
        position: Int
    ): Any {
        val view: View
        val imageView: ImageView
        val item = items[position]

        onItem(false)

        if (item.type == MessageAttachment.TYPE_IMAGE) { // TYPE_IMAGE
            val binding =
                ItemContactChatMediaDetailImageBinding.inflate(layoutInflater, container, false)
            view = binding.root
            imageView = binding.zmImage
            Glide.with(view).load(item.file).into(imageView)
        } else { // TYPE_FILE
            val binding =
                ItemContactChatMediaDetailDocumentBinding.inflate(layoutInflater, container, false)
            view = binding.root
            imageView = binding.ivAttachment
            binding.tvFileName.text = item.fileName
            binding.tvFileSize.text = getFormattedSize(item.file.length().toDouble())
        }

        imageView.setOnTouchListener(
            object : OnSwipeTouchListener(view.context) {
                override fun onSwipeDown() {
                    if ((item.type == MessageAttachment.TYPE_IMAGE && (imageView as ZoomageView).currentScaleFactor == 1.0f)) {
                        onItem(true)
                    } else if (item.type == MessageAttachment.TYPE_FILE) {
                        onItem(true)
                    }
                }
            }
        )

        container.addView(view)

        return view
    }

    override fun destroyItem(
        container: ViewGroup,
        position: Int,
        `object`: Any
    ) {
        container.removeView(`object` as LinearLayout)
    }
}
