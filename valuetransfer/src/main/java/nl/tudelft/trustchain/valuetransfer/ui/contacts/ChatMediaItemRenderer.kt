package nl.tudelft.trustchain.valuetransfer.ui.contacts

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_contact_chat_media_gallery.view.*
import nl.tudelft.trustchain.valuetransfer.util.MessageAttachment
import nl.tudelft.trustchain.valuetransfer.R

class ChatMediaItemRenderer(
    private val onItemClick: (ChatMediaItem) -> Unit
) : ItemLayoutRenderer<ChatMediaItem, View>(
    ChatMediaItem::class.java
) {

    override fun bindView(item: ChatMediaItem, view: View) = with(view) {
        val fileNameView = view.findViewById<TextView>(R.id.tvFileName)
        when (item.type) {
            MessageAttachment.TYPE_IMAGE -> {
                fileNameView.isVisible = false
                if (item.file.exists()) {
                    Glide.with(view).load(item.file).into(ivImageItem)
                } else {
                    val drawable = ContextCompat.getDrawable(view.context, R.drawable.ic_media)
                    ivImageItem.setImageDrawable(drawable)
                }
                view.findViewById<ImageView>(R.id.ivMediaTypeImage).isVisible = true
                view.findViewById<ImageView>(R.id.ivMediaTypeFile).isVisible = false
            }
            MessageAttachment.TYPE_FILE -> {
                ivImageItem.setImageDrawable(null)
                fileNameView.isVisible = true
                fileNameView.text = item.fileName
                view.findViewById<ImageView>(R.id.ivMediaTypeImage).isVisible = false
                view.findViewById<ImageView>(R.id.ivMediaTypeFile).isVisible = true
            }
        }

        clImageContainer.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_contact_chat_media_gallery
    }
}
