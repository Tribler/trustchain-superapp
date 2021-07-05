package nl.tudelft.trustchain.valuetransfer.ui.contacts

import android.util.Log
import android.view.View
import com.bumptech.glide.Glide
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_contact_chat_message.view.*
import nl.tudelft.trustchain.peerchat.ui.conversation.ChatMessageItem
import nl.tudelft.trustchain.peerchat.ui.conversation.MessageAttachment
import nl.tudelft.trustchain.valuetransfer.R
import java.text.SimpleDateFormat

class ContactChatItemRenderer(
    private val onItemClick: (ChatMessageItem) -> Unit
) : ItemLayoutRenderer<ChatMessageItem, View>(
    ChatMessageItem::class.java
) {

    override fun bindView(item: ChatMessageItem, view: View) = with(view) {

        val messageDate = item.chatMessage.timestamp

        if(!item.shouldShowDate) {
            clDateSection.visibility = View.GONE
        }else{
            tvDateSection.text = SimpleDateFormat("EEEE, d MMM").format(messageDate)
        }

        val dateFormat = SimpleDateFormat("HH:mm")

        val attachment = item.chatMessage.attachment
        val transaction = item.transaction?.transaction

        Log.d("TESTJE", "TRANSACTION: ${transaction}")

        if(item.chatMessage.outgoing) { // OUTGOING
            if(attachment != null) {
                when(attachment.type) {
                    MessageAttachment.TYPE_IMAGE -> {
                        clMessageSection.visibility = View.GONE
                        clAttachmentPhotoVideoIncoming.visibility = View.GONE

                        val file = attachment.getFile(view.context)
                        if (file.exists()) {
                            Glide.with(view).load(file).into(ivAttachmentPhotoVideoOutgoing)
                            ivAttachmentPhotoVideoOutgoing.clipToOutline = true
                        } else {
                            ivAttachmentPhotoVideoOutgoing.setImageBitmap(null)
                        }
                    }
                }
            }else{ // MESSAGE
                tvChatMessageOutgoing.text = item.chatMessage.message
                tvChatMessageTimeOutgoing.text = dateFormat.format(messageDate)

                clMessageIncoming.visibility = View.GONE
                clAttachmentPhotoVideoSection.visibility = View.GONE
            }

            if(item.chatMessage.ack) { // SEND AND RECEIVED RECEIPT
                ivMessageStatus.setImageResource(R.drawable.ic_check_double)
                ivAttachmentPhotoVideoStatus.setImageResource((R.drawable.ic_check_double))
            }else{
                ivMessageStatus.setImageResource(R.drawable.ic_check_single)
                ivAttachmentPhotoVideoStatus.setImageResource(R.drawable.ic_check_single)
            }
        } else { // INCOMING
            if(attachment != null) {
                when(attachment.type) {
                    MessageAttachment.TYPE_IMAGE -> { // ATTACHMENT PHOTO VIDEO
                        clMessageSection.visibility = View.GONE
                        clAttachmentPhotoVideoOutgoing.visibility = View.GONE

                        val file = attachment.getFile(view.context)
                        if (file.exists()) {
                            Glide.with(view).load(file).into(ivAttachmentPhotoVideoIncoming)
                            ivAttachmentPhotoVideoIncoming.clipToOutline = true
                        } else {
                            ivAttachmentPhotoVideoIncoming.setImageBitmap(null)
                        }
                    }
                }
            }else{ // MESSAGE
                tvChatMessageIncoming.text = item.chatMessage.message
                tvChatMessageTimeIncoming.text = dateFormat.format(messageDate)

                clAttachmentPhotoVideoSection.visibility = View.GONE
                clMessageOutgoing.visibility = View.GONE
            }
        }

        setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_contact_chat_message
    }
}
