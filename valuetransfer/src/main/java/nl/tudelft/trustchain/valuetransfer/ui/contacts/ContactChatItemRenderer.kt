package nl.tudelft.trustchain.valuetransfer.ui.contacts

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.Glide
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.fragment_contacts_chat.view.*
import kotlinx.android.synthetic.main.item_contacts_chat_detail.view.*
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.peerchat.ui.conversation.ChatMessageItem
import nl.tudelft.trustchain.peerchat.ui.conversation.MessageAttachment
import nl.tudelft.trustchain.valuetransfer.R
import java.math.BigInteger
import java.text.SimpleDateFormat

class ContactChatItemRenderer(
    private val onItemClick: (ChatMessageItem) -> Unit
) : ItemLayoutRenderer<ChatMessageItem, View>(
    ChatMessageItem::class.java
) {

    val dateFormat = SimpleDateFormat("HH:mm")

    override fun bindView(item: ChatMessageItem, view: View) = with(view) {

        val messageDate = item.chatMessage.timestamp

        clDateSection.isVisible = item.shouldShowDate
        tvDateSection.text = SimpleDateFormat("EEEE, d MMM").format(messageDate)

        val attachment = item.chatMessage.attachment

        tvChatMessage.text = item.chatMessage.message
        tvChatItemTime.text = dateFormat.format(messageDate)
        ivChatItemStatus.isVisible = item.chatMessage.outgoing

        val itemStatusResource = when {
            item.chatMessage.ack -> R.drawable.ic_check_double
            else -> R.drawable.ic_check_single
        }

        ivChatItemStatus.setImageResource(itemStatusResource)

        val textColor: Int
        val backgroundResource: Int

        when {
            item.chatMessage.outgoing -> {
                backgroundResource = R.drawable.pill_rounded_dark_green
                textColor = R.color.white
            }
            else -> {
                backgroundResource = R.drawable.pill_rounded_white
                textColor = R.color.black
            }
        }

        clChatMessageTransaction.setBackgroundResource(backgroundResource)
        tvTransactionTitle.setTextColor(ContextCompat.getColor(this.context, textColor))
        tvTransaction.setTextColor(ContextCompat.getColor(this.context, textColor))
        tvChatMessage.setTextColor(ContextCompat.getColor(this.context, textColor))

        when {
            attachment != null -> {
                when (attachment.type) {
                    MessageAttachment.TYPE_IMAGE -> {
                        clTransaction.isVisible = false
                        clChatMessageTransaction.isVisible = false
                        ivAttachmentPhotoVideo.isVisible = true

                        val file = attachment.getFile(view.context)
                        if (file.exists()) {
                            Glide.with(view).load(file).into(ivAttachmentPhotoVideo)
                            ivAttachmentPhotoVideo.clipToOutline = true
                        } else {
                            ivAttachmentPhotoVideo.setImageBitmap(null)
                        }

                        ivAttachmentPhotoVideo.setImageResource(if (item.chatMessage.ack) R.drawable.ic_check_double else R.drawable.ic_check_single)
                    }
                }
            }
            item.chatMessage.transactionHash != null -> {
                clTransaction.isVisible = true
                tvChatMessage.isVisible = item.chatMessage.message.isNotBlank()
                ivAttachmentPhotoVideo.isVisible = false

                tvTransactionTitle.text = if(item.chatMessage.outgoing) "Transferred" else "Received"
                tvTransaction.text = TransactionRepository.prettyAmount((item.transaction?.transaction?.get("amount") as BigInteger).toLong())

            }
            else -> {
                clTransaction.isVisible = false
                tvChatMessage.isVisible = true
                ivAttachmentPhotoVideo.isVisible = false
            }
        }

        val constraintSet = ConstraintSet()
        constraintSet.clone(clItem)
        constraintSet.removeFromHorizontalChain(flContent.id)
        constraintSet.removeFromHorizontalChain(llChatItemTimeStatus.id)

        constraintSet.connect(flContent.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(flContent.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        val gravityHorizontal = if(item.chatMessage.outgoing) Gravity.END else Gravity.START
        val constraintGravity = if(item.chatMessage.outgoing) ConstraintSet.END else ConstraintSet.START

        llChatItemTimeStatus.gravity = gravityHorizontal
        tvTransaction.gravity = gravityHorizontal
        tvChatMessage.gravity = gravityHorizontal

        flChatItemContent.updateLayoutParams<FrameLayout.LayoutParams> {
            gravity = gravityHorizontal
        }

        constraintSet.connect(llChatItemTimeStatus.id, constraintGravity, ConstraintSet.PARENT_ID, constraintGravity)
        constraintSet.applyTo(clItem)

        setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_contacts_chat_detail
    }
}
