package nl.tudelft.trustchain.valuetransfer.ui.contacts

import android.view.View
import androidx.core.view.isVisible
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_contacts_chat.view.*
import kotlinx.android.synthetic.main.item_contacts_chat.view.ivIdenticon
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.util.getColorByHash
import nl.tudelft.trustchain.peerchat.ui.conversation.MessageAttachment
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.util.generateIdenticon
import java.text.SimpleDateFormat
import java.util.*

class ChatItemRenderer(
    private val onChatClick: (Contact) -> Unit,
) : ItemLayoutRenderer<ChatItem, View>(
    ChatItem::class.java
) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.ENGLISH)
    private val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.ENGLISH)
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)

    override fun bindView(item: ChatItem, view: View) = with(view) {

        val lastMessageDate = item.lastMessage?.timestamp
        val diff = Date().time - lastMessageDate!!.time

        val message = when {
            item.lastMessage.transactionHash != null -> this.context.resources.getString(R.string.text_attachment_transaction)
            item.lastMessage.attachment != null -> when (item.lastMessage.attachment?.type) {
                MessageAttachment.TYPE_IMAGE -> this.context.resources.getString(R.string.text_attachment_photo)
                MessageAttachment.TYPE_FILE -> this.context.resources.getString(R.string.text_attachment_file)
                MessageAttachment.TYPE_IDENTITY_ATTRIBUTE -> this.context.resources.getString(R.string.text_attachment_identity_attribute)
                MessageAttachment.TYPE_LOCATION -> this.context.resources.getString(R.string.text_attachment_location)
                MessageAttachment.TYPE_TRANSFER_REQUEST -> this.context.resources.getString(R.string.text_attachment_transfer_request)
                MessageAttachment.TYPE_CONTACT -> this.context.resources.getString(R.string.text_attachment_contact)
                else -> this.context.resources.getString(R.string.text_attachment)
            }
            else -> item.lastMessage.message
        }

        val contactTime = when {
            diff <= 24 * 60 * 60 * 1000 -> timeFormat.format(lastMessageDate)
            diff <= 7 * 24 * 60 * 60 * 1000 -> dayOfWeekFormat.format(lastMessageDate)
            else -> dateFormat.format(lastMessageDate)
        }

        if (item.image == null) {
            item.contact.publicKey.keyToBin().toHex().let { publicKeyString ->
                generateIdenticon(
                    publicKeyString.substring(20, publicKeyString.length).toByteArray(),
                    getColorByHash(context, publicKeyString),
                    resources
                ).let {
                    ivContactImage.isVisible = false
                    ivIdenticon.apply {
                        isVisible = true
                        setImageBitmap(it)
                    }
                }
            }
        } else {
            ivIdenticon.isVisible = false
            ivContactImage.apply {
                isVisible = true
                setImageBitmap(item.image.image)
            }
        }

        ivContactVerifiedStatus.isVisible = item.state?.identityInfo?.isVerified == true
        ivContactUnverifiedStatus.isVisible = item.state?.identityInfo?.isVerified == false

        if (item.lastMessage.outgoing) {
            if (item.lastMessage.ack) {
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
        ivOfflineStatusOverview.isVisible = !item.isOnline && !item.isBluetooth
        tvContactTimeOverview.text = contactTime

        ivMuted.isVisible = item.state?.isMuted ?: false

        clChatCard.setOnClickListener {
            onChatClick(item.contact)
        }
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_contacts_chat
    }
}
