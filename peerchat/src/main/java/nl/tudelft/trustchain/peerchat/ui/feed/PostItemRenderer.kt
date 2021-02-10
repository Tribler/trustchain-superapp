package nl.tudelft.trustchain.peerchat.ui.feed

import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_post.view.*
import nl.tudelft.trustchain.peerchat.R
import java.text.SimpleDateFormat

class PostItemRenderer(
    private val onLike: (PostItem) -> Unit,
    private val onComment: (PostItem) -> Unit
) : ItemLayoutRenderer<PostItem, View>(
    PostItem::class.java
) {
    private val dateFormat = SimpleDateFormat.getDateTimeInstance()

    override fun bindView(item: PostItem, view: View) = with(view) {
        val prefix = if (item.linkedContact != null) "@" + item.linkedContact.name + ": " else ""
        val text = item.block.transaction[PostRepository.KEY_TEXT] as? String
        txtMessage.text = prefix + text

        val lastMessageDate = item.block.timestamp
        txtDate.text = dateFormat.format(lastMessageDate)

        txtName.text = item.contact?.name

        txtPeerId.text = item.contact?.mid

        if (item.contact != null) {
            avatar.setUser(item.contact.mid, item.contact.name)
        }

        btnLike.setOnClickListener {
            onLike(item)
        }

        btnComment.setOnClickListener {
            onComment(item)
        }

        txtCommentCount.text = item.replies.size.toString()
        txtLikeCount.text = item.likes.size.toString()

        val likeColor = if (item.liked)
            ResourcesCompat.getColor(view.resources, R.color.colorPrimary, null) else
            ResourcesCompat.getColor(view.resources, R.color.text_secondary, null)

        btnLike.setColorFilter(likeColor)
        txtLikeCount.setTextColor(likeColor)

        btnComment.isVisible = item.block.isProposal
        txtCommentCount.isVisible = item.block.isProposal

        /*
        // txtName.isVisible = item.contact.name.isNotEmpty()
        txtPeerId.text = item.contact.mid
        txtMessage.isVisible = item.lastMessage != null
        txtMessage.text = item.lastMessage?.message
        val textStyle = if (item.lastMessage?.read == false) Typeface.BOLD else Typeface.NORMAL
        txtMessage.setTypeface(null, textStyle)
        setOnClickListener {
            onItemClick(item.contact)
        }
        imgWifi.isVisible = item.isOnline
        imgBluetooth.isVisible = item.isBluetooth
        avatar.setUser(item.contact.mid, item.contact.name)
        setOnLongClickListener {
            onItemLongClick(item.contact)
            true
        }
         */
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_post
    }
}
