package nl.tudelft.trustchain.valuetransfer.ui.contacts

import android.view.View
import androidx.core.view.isVisible
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_contacts_chat.view.*
import kotlinx.android.synthetic.main.item_contacts_chat.view.ivIdenticon
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.util.getColorByHash
import nl.tudelft.trustchain.peerchat.ui.contacts.ContactItem
import nl.tudelft.trustchain.peerchat.ui.conversation.MessageAttachment
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.util.generateIdenticon
import java.text.SimpleDateFormat
import java.util.*

class ChatItemRenderer(
    private val onChatClick: (Contact) -> Unit,
) : ItemLayoutRenderer<ContactItem, View>(
    ContactItem::class.java
) {

    private val timeFormat = SimpleDateFormat("HH:mm")
    private val dayOfWeekFormat = SimpleDateFormat("EEEE")
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy")

    override fun bindView(item: ContactItem, view: View) = with(view) {

        val lastMessageDate = item.lastMessage?.timestamp
        val diff = Date().time - lastMessageDate!!.time

        val message = when {
            item.lastMessage?.transactionHash != null -> "Transaction"
            item.lastMessage?.attachment != null -> when (item.lastMessage?.attachment?.type) {
                MessageAttachment.TYPE_IMAGE -> "Attachment: Photo/Video"
                MessageAttachment.TYPE_IDENTITY_ATTRIBUTE -> "Attachment: Identity Attribute"
                MessageAttachment.TYPE_LOCATION -> "Attachment: Location"
                MessageAttachment.TYPE_FILE -> "Attachment: File"
                MessageAttachment.TYPE_TRANSFER_REQUEST -> "Attachment: Transfer Request"
                MessageAttachment.TYPE_CONTACT -> "Attachment: Contact"
                else -> "Attachment"
            }
            else -> item.lastMessage?.message
        }

        val contactTime = when {
            diff <= 24 * 60 * 60 * 1000 -> timeFormat.format(lastMessageDate)
            diff <= 7 * 24 * 60 * 60 * 1000 -> dayOfWeekFormat.format(lastMessageDate)
            else -> dateFormat.format(lastMessageDate)
        }

        val publicKeyString = item.contact.publicKey.toString()
        val input = publicKeyString.substring(20, publicKeyString.length).toByteArray()
        val color = getColorByHash(context, publicKeyString)
        val identicon = generateIdenticon(input, color, resources)
        ivIdenticon.setImageBitmap(identicon)

        if (item.lastMessage?.outgoing!!) {
            if (item.lastMessage?.ack!!) {
                ivMessageStatusOverview.setImageResource(R.drawable.ic_check_double)
            } else {
                ivMessageStatusOverview.setImageResource(R.drawable.ic_check_single)
            }
        } else {
            ivMessageStatusOverview.isVisible = false
        }

        tvContactNameOverview.text = item.contact.name
        tvContactMessageOverview.text = message

        ivOnlineStatusOverview.isVisible = item.isOnline || item.isBluetooth
        tvContactTimeOverview.text = contactTime

        clChatCard.setOnClickListener {
            onChatClick(item.contact)
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_contacts_chat
    }
}
