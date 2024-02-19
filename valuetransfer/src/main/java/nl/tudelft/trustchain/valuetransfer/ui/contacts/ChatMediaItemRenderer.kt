package nl.tudelft.trustchain.valuetransfer.ui.contacts

import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.mattskala.itemadapter.ItemLayoutRenderer
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.databinding.ItemContactChatMediaGalleryBinding
import nl.tudelft.trustchain.valuetransfer.util.MessageAttachment

class ChatMediaItemRenderer(
    private val onItemClick: (ChatMediaItem) -> Unit
) : ItemLayoutRenderer<ChatMediaItem, View>(
        ChatMediaItem::class.java
    ) {
    override fun bindView(
        item: ChatMediaItem,
        view: View
    ) = with(view) {
        val binding = ItemContactChatMediaGalleryBinding.bind(view)
        val fileNameView = binding.tvFileName
        val ivImageItem = binding.ivImageItem
        val clImageContainer = binding.clImageContainer
        when (item.type) {
            MessageAttachment.TYPE_IMAGE -> {
                fileNameView.isVisible = false
                if (item.file.exists()) {
                    Glide.with(view).load(item.file).into(ivImageItem)
                } else {
                    val drawable = ContextCompat.getDrawable(view.context, R.drawable.ic_media)
                    ivImageItem.setImageDrawable(drawable)
                }
                binding.ivMediaTypeImage.isVisible = true
                binding.ivMediaTypeFile.isVisible = false
            }

            MessageAttachment.TYPE_FILE -> {
                ivImageItem.setImageDrawable(null)
                fileNameView.isVisible = true
                fileNameView.text = item.fileName
                binding.ivMediaTypeImage.isVisible = false
                binding.ivMediaTypeFile.isVisible = true
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
