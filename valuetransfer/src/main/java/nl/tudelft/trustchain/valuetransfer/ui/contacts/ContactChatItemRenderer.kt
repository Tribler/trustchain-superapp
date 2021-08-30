package nl.tudelft.trustchain.valuetransfer.ui.contacts

import android.os.Handler
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.*
import com.bumptech.glide.Glide
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_contacts_chat_detail.view.*
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.util.TrustChainHelper
import nl.tudelft.trustchain.common.util.getColorByHash
import nl.tudelft.trustchain.peerchat.ui.conversation.ChatMessageItem
import nl.tudelft.trustchain.peerchat.ui.conversation.MessageAttachment
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.entity.IdentityAttribute
import nl.tudelft.trustchain.valuetransfer.util.formatBalance
import nl.tudelft.trustchain.valuetransfer.util.generateIdenticon
import org.json.JSONObject
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*

class ContactChatItemRenderer(
    private val parentActivity: ValueTransferMainActivity,
    private val onItemClick: (ChatMessageItem) -> Unit,
    private val onLoadMoreClick: (ChatMessageItem) -> Unit,
) : ItemLayoutRenderer<ChatMessageItem, View>(
    ChatMessageItem::class.java
) {
    private val timeFormat = SimpleDateFormat("HH:mm")
    private val dateFormat = SimpleDateFormat("EEEE, d MMM")
    private val previousYearsDateFormat = SimpleDateFormat("EEEE, d MMM yyyy")
    private val yearFormat = SimpleDateFormat("yyyy")

    override fun bindView(item: ChatMessageItem, view: View) = with(view) {

        // Necessary to hide content types because the recycler view displays wrong data sometimes
        clChatMessage.isVisible = false
        clTransaction.isVisible = false
        clAttachmentPhotoVideo.isVisible = false
        clAttachmentIdentityAttribute.isVisible = false
        clAttachmentFile.isVisible = false
        clAttachmentLocation.isVisible = false
        clAttachmentTransferRequest.isVisible = false
        clAttachmentContact.isVisible = false

        tvChatMessage.text = ""
        tvChatMessage.isVisible = false
        tvTransactionMessage.text = ""
        tvTransactionMessage.isVisible = false
        tvAttachmentTransferRequestDescription.text = ""
        tvAttachmentTransferRequestDescription.isVisible = false
        tvAttachmentContactName.text = ""
        tvAttachmentContactName.isVisible = false
        tvAttachmentContactPublicKey.text = ""
        tvAttachmentContactPublicKey.isVisible = false

        // ShouldShowAvatar boolean is reused as the button to load more messages in chat
        clLoadMoreMessagesSection.isVisible = item.shouldShowAvatar
        if (item.shouldShowAvatar) {
            btnLoadMoreMessages.setOnClickListener {
                onLoadMoreClick(item)
            }
        }

        // Date section when item is the first of the data
        val messageDate = item.chatMessage.timestamp
        clDateSection.isVisible = item.shouldShowDate
        tvDateSection.text = if(yearFormat.format(Date()).equals(yearFormat.format(messageDate))) {
            dateFormat.format(messageDate)
        }else{
            previousYearsDateFormat.format(messageDate)
        }

        // Chat item time and status
        tvChatItemTime.text = timeFormat.format(messageDate)

        if (item.chatMessage.outgoing) {
            ivChatItemStatus.isVisible = item.chatMessage.outgoing

            val itemStatusResource = when {
                item.chatMessage.ack -> R.drawable.ic_check_double
                else -> R.drawable.ic_check_single
            }

            ivChatItemStatus.setImageResource(itemStatusResource)
        }

        // Sender and receiver have different background and text colors
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

        // Show simple chat message
        if(item.chatMessage.message.isNotBlank() && item.chatMessage.attachment == null && item.chatMessage.transactionHash == null) {
            clChatMessage.isVisible = true
            tvChatMessage.isVisible = true

            clChatMessage.setBackgroundResource(backgroundResource)
            tvChatMessage.setTextColor(ContextCompat.getColor(this.context, textColor))

            tvChatMessage.text = item.chatMessage.message
        }

        // Show the different types of attachments
        item.chatMessage.attachment?.let { attachment ->
            when (attachment.type) {
                MessageAttachment.TYPE_IMAGE -> {
                    clAttachmentPhotoVideo.isVisible = true
                    clAttachmentPhotoVideo.setBackgroundResource(backgroundResource)

                    val file = attachment.getFile(view.context)
                    if (file.exists()) {
                        Glide.with(view).load(file).into(ivAttachmentPhotoVideo)
                        ivAttachmentPhotoVideo.clipToOutline = true
                    } else {
                        ivAttachmentPhotoVideo.setImageBitmap(null)
                    }

                    ivAttachmentPhotoVideo.setOnClickListener {
                        onItemClick(item)
                    }
                }
                MessageAttachment.TYPE_FILE -> {
                    clAttachmentFile.isVisible = true

                    clAttachmentFile.setBackgroundResource(backgroundResource)
                    tvAttachmentFileTitle.setTextColor(ContextCompat.getColor(this.context, textColor))
                    tvAttachmentFilename.setTextColor(ContextCompat.getColor(this.context, textColor))

                    val file = attachment.getFile(view.context)
                    if (file.exists()) {
                        tvAttachmentFilename.text = item.chatMessage.message

                        clAttachmentFile.setOnClickListener {
                            onItemClick(item)
                        }
                    }
                }
                MessageAttachment.TYPE_CONTACT -> {
                    clAttachmentContact.isVisible = true
                    tvAttachmentContactName.isVisible = true
                    tvAttachmentContactPublicKey.isVisible = true

                    clAttachmentContact.setBackgroundResource(backgroundResource)
                    tvAttachmentContactName.setTextColor(ContextCompat.getColor(this.context, textColor))
                    tvAttachmentContactPublicKey.setTextColor(ContextCompat.getColor(this.context, textColor))

                    val contact = Contact.deserialize(attachment.content, 0).first
                    tvAttachmentContactName.text = contact.name
                    val publicKey = contact.publicKey.keyToBin().toHex()
                    tvAttachmentContactPublicKey.text = publicKey

                    val input = publicKey.substring(20, publicKey.length).toByteArray()
                    val color = getColorByHash(context, publicKey)
                    val identicon = generateIdenticon(input, color , resources)
                    ivAttachmentContactIdenticon.setImageBitmap(identicon)

                    if(!item.chatMessage.outgoing) {
                        clAttachmentContactIcon.isVisible = true
                        clAttachmentContact.setOnClickListener {
                            onItemClick(item)
                        }
                    }
                }
                MessageAttachment.TYPE_LOCATION -> {
                    clAttachmentLocation.isVisible = true

                    clAttachmentLocation.setBackgroundResource(backgroundResource)
                    tvLocation.setTextColor(ContextCompat.getColor(this.context, textColor))

                    val offsetBuffer = attachment.content.copyOfRange(0, attachment.content.size)
                    JSONObject(offsetBuffer.decodeToString()).let { json ->
                        tvLocation.text = json.getString("address_line").replace(", ", "\n")

                        clAttachmentLocation.setOnClickListener {
                            onItemClick(item)
                        }
                    }
                }
                MessageAttachment.TYPE_IDENTITY_ATTRIBUTE -> {
                    clAttachmentIdentityAttribute.isVisible = true

                    clAttachmentIdentityAttribute.setBackgroundResource(backgroundResource)
                    tvIdentityAttributeName.setTextColor(ContextCompat.getColor(this.context, textColor))
                    tvIdentityAttributeValue.setTextColor(ContextCompat.getColor(this.context, textColor))

                    val identityAttribute = IdentityAttribute.deserialize(attachment.content, 0).first
                    tvIdentityAttributeName.text = identityAttribute.name
                    tvIdentityAttributeValue.text = identityAttribute.value

                    if (!item.chatMessage.outgoing) {
                        clAttachmentIdentityAttribute.setOnClickListener {
                            onItemClick(item)
                        }
                    }
                }
                MessageAttachment.TYPE_TRANSFER_REQUEST -> {
                    clAttachmentTransferRequest.isVisible = true

                    clAttachmentTransferRequest.setBackgroundResource(backgroundResource)
                    tvAttachmentTransferRequestTitle.setTextColor(ContextCompat.getColor(this.context, textColor))
                    tvAttachmentTransferRequestDescription.setTextColor(ContextCompat.getColor(this.context, textColor))

                    val offsetBuffer = attachment.content.copyOfRange(0, attachment.content.size)
                    JSONObject(offsetBuffer.decodeToString()).let { json ->
                        tvAttachmentTransferRequestTitle.text = "Request to transfer ${formatBalance(json.optLong("amount"))} ET"

                        item.chatMessage.message.let { description ->
                            tvAttachmentTransferRequestDescription.isVisible = description.isNotEmpty()
                            tvAttachmentTransferRequestDescription.text = description
                        }

                        if (!item.chatMessage.outgoing) {
                            ivAttachmentTransferRequestContinueIcon.isVisible = true

                            clAttachmentTransferRequest.setOnClickListener {
                                onItemClick(item)
                            }
                        }
                    }
                }
            }
        }

        // Show the transaction w/o its message
        item.chatMessage.transactionHash?.let { transactionHash ->
            clTransaction.isVisible = true
            clTransaction.setBackgroundResource(backgroundResource)
            tvTransactionTitle.setTextColor(ContextCompat.getColor(this.context, textColor))

            val isReceived = if(item.chatMessage.outgoing) {
                val trustChainHelper = parentActivity.getStore(ValueTransferMainActivity.trustChainHelperTag) as TrustChainHelper
                val transactionRepository = parentActivity.getStore(ValueTransferMainActivity.transactionRepositoryTag) as TransactionRepository

                val transaction = transactionRepository.getTransactionWithHash(transactionHash)
                val blocks = trustChainHelper.getChainByUser(trustChainHelper.getMyPublicKey())

                blocks.find { it.linkedBlockId == transaction!!.blockId} != null
            }else{
                false
            }

            if (item.chatMessage.message.isNotEmpty()) {
                tvTransactionMessage.isVisible = true
                tvTransactionMessage.text = item.chatMessage.message
                tvTransactionMessage.setTextColor(ContextCompat.getColor(this.context, textColor))
            }

            if (item.chatMessage.outgoing && !isReceived) {
                clTransactionResend.isVisible = true
                clTransactionResend.setOnClickListener {
                    onItemClick(item)
                    clTransactionResend.background = ContextCompat.getDrawable(this.context, R.drawable.pill_rounded_bottom_orange)
                    tvResendTransaction.text = "Trying to resend..."
                    Handler().postDelayed(
                        Runnable {
                            clTransactionResend.background = ContextCompat.getDrawable(this.context, R.drawable.pill_rounded_bottom_red)
                            tvResendTransaction.text = "Resend Transaction"
                        }, 10000
                    )
                }
            }

            ivTransactionIconIncoming.isVisible = !item.chatMessage.outgoing
            ivTransactionIconOutgoing.isVisible = item.chatMessage.outgoing

            tvTransactionTitle.text = if (item.transaction?.transaction?.get("amount") != null) {
                    val amount = formatBalance((item.transaction?.transaction?.get("amount") as BigInteger).toLong())

                    if (item.chatMessage.outgoing) {
                        "Outgoing transfer of $amount ET"
                    } else {
                        "Incoming transfer of $amount ET"
                    }
                } else {
                    if (item.chatMessage.outgoing) {
                        "Outgoing transfer failed"
                    } else {
                        "Unknown incoming transfer, ask to resend transfer"
                    }
                }
        }

        // Position the message or attachments on the left or right side of the view, depending on incoming or outgoing
        val constraintSet = ConstraintSet()
        constraintSet.clone(clItem)
        constraintSet.removeFromHorizontalChain(flContent.id)
        constraintSet.removeFromHorizontalChain(llChatItemTimeStatus.id)

        constraintSet.connect(flContent.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(flContent.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        val gravityHorizontal = if(item.chatMessage.outgoing) Gravity.END else Gravity.START
        val constraintGravity = if(item.chatMessage.outgoing) ConstraintSet.END else ConstraintSet.START

        llChatItemTimeStatus.gravity = gravityHorizontal

        flContent.setPadding(0, flContent.paddingTop, 0, flContent.paddingBottom)

        if(item.chatMessage.outgoing) {
            flContent.setPadding(100, flContent.paddingTop, flContent.paddingRight, flContent.paddingBottom)
        }else{
            flContent.setPadding(flContent.paddingLeft, flContent.paddingTop, 100, flContent.paddingBottom)
        }

        flChatItemContent.updateLayoutParams<FrameLayout.LayoutParams> {
            gravity = gravityHorizontal
        }

        constraintSet.connect(llChatItemTimeStatus.id, constraintGravity, ConstraintSet.PARENT_ID, constraintGravity)
        constraintSet.applyTo(clItem)
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_contacts_chat_detail
    }
}
