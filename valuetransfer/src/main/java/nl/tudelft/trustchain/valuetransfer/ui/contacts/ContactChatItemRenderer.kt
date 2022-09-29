package nl.tudelft.trustchain.valuetransfer.ui.contacts

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.*
import com.bumptech.glide.Glide
import com.mattskala.itemadapter.ItemLayoutRenderer
import kotlinx.android.synthetic.main.item_contacts_chat_detail.view.*
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.messaging.eva.TransferState
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.util.getColorByHash
import nl.tudelft.trustchain.peerchat.ui.conversation.MessageAttachment
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.common.valuetransfer.entity.IdentityAttribute
import nl.tudelft.trustchain.common.valuetransfer.entity.TransferRequest
import nl.tudelft.trustchain.peerchat.community.PeerChatCommunity
import nl.tudelft.trustchain.valuetransfer.util.formatBalance
import nl.tudelft.trustchain.valuetransfer.util.generateIdenticon
import nl.tudelft.trustchain.valuetransfer.util.getColorIDFromThemeAttribute
import nl.tudelft.trustchain.valuetransfer.util.getFormattedSize
import org.json.JSONObject
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*

class ContactChatItemRenderer(
    private val parentActivity: ValueTransferMainActivity,
    private val onItemClick: (ContactChatItem) -> Unit,
    private val onProgressClick: (Pair<PublicKey, String>) -> Unit,
    private val onLoadMoreClick: (ContactChatItem) -> Unit,
) : ItemLayoutRenderer<ContactChatItem, View>(
    ContactChatItem::class.java
) {
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.ENGLISH)
    private val dateFormat = SimpleDateFormat("EEEE, d MMM", Locale.ENGLISH)
    private val previousYearsDateFormat = SimpleDateFormat("EEEE, d MMM yyyy", Locale.ENGLISH)
    private val yearFormat = SimpleDateFormat("yyyy", Locale.ENGLISH)

    override fun bindView(item: ContactChatItem, view: View) = with(view) {
        // Necessary to hide content types because the recycler view sometimes displays wrong data
        clChatMessage.isVisible = false
        clTransaction.isVisible = false
        clAttachmentPhotoVideo.isVisible = false
        clAttachmentFile.isVisible = false
        clAttachmentIdentityAttribute.isVisible = false
        clAttachmentLocation.isVisible = false
        clAttachmentTransferRequest.isVisible = false
        clAttachmentContact.isVisible = false
        clIdentityUpdated.isVisible = false
        clAttachmentProgress.isVisible = false
        llChatItemTimeStatus.isVisible = true

        tvChatMessage.text = ""
        tvChatMessage.isVisible = false
        tvTransactionMessage.text = ""
        tvTransactionMessage.isVisible = false
        ivTransactionErrorIcon.isVisible = false
        tvAttachmentProgress.isVisible = false
        tvAttachmentProgress.text = ""
        tvAttachmentProgressStatus.isVisible = false
        tvAttachmentProgressStatus.text = ""
        ivAttachmentProgressPlay.isVisible = false
        ivAttachmentProgressStop.isVisible = false
        tvAttachmentProgressSize.isVisible = false
        tvAttachmentProgressSize.text = ""
        tvAttachmentProgressType.isVisible = false
        tvAttachmentProgressType.text = ""
        tvAttachmentFileName.text = ""
        tvAttachmentFileName.isVisible = false
        tvAttachmentFileSize.text = ""
        tvAttachmentFileSize.isVisible = false
        tvAttachmentTransferRequestDescription.text = ""
        tvAttachmentTransferRequestDescription.isVisible = false
        tvAttachmentContactName.text = ""
        tvAttachmentContactName.isVisible = false
        tvAttachmentContactPublicKey.text = ""
        tvAttachmentContactPublicKey.isVisible = false
        tvIdentityUpdatedMessage.text = ""
        ivChatItemStatus.isVisible = false

        // Show the load more messages button
        clLoadMoreMessagesSection.isVisible = item.loadMoreMessages
        if (item.loadMoreMessages) {
            btnLoadMoreMessages.setOnClickListener {
                onLoadMoreClick(item)
            }
        }

        // Date section when item is the first of the data
        val messageDate = item.chatMessage.timestamp
        clDateSection.isVisible = item.shouldShowDate
        tvDateSection.text = if (yearFormat.format(Date()).equals(yearFormat.format(messageDate))) {
            dateFormat.format(messageDate)
        } else {
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
                backgroundResource = R.drawable.pill_rounded_chat_from
                textColor = getColorIDFromThemeAttribute(parentActivity, R.attr.onChatFromColor)
            }
            else -> {
                backgroundResource = R.drawable.pill_rounded_chat_to
                textColor = getColorIDFromThemeAttribute(parentActivity, R.attr.onChatToColor)
            }
        }

        // Show simple chat message
        if (item.chatMessage.message.isNotBlank() && item.chatMessage.attachment == null && item.chatMessage.transactionHash == null) {
            clChatMessage.isVisible = true
            tvChatMessage.isVisible = true

            clChatMessage.setBackgroundResource(backgroundResource)
            tvChatMessage.setTextColor(ContextCompat.getColor(this.context, textColor))

            tvChatMessage.text = item.chatMessage.message
        }

        // Show the different types of attachments
        item.chatMessage.attachment?.let { attachment ->
            val size = getFormattedSize(item.chatMessage.attachment?.size!!.toDouble())

            when (attachment.type) {
                MessageAttachment.TYPE_FILE -> {
                    if (item.chatMessage.attachmentFetched) {
                        clAttachmentProgress.isVisible = false
                        clAttachmentFile.isVisible = true
                        clAttachmentFile.setBackgroundResource(backgroundResource)

                        tvAttachmentFileName.isVisible = true
                        tvAttachmentFileName.text = item.chatMessage.message
                        tvAttachmentFileName.setTextColor(ContextCompat.getColor(this.context, textColor))

                        tvAttachmentFileSize.isVisible = item.chatMessage.attachment?.size != null
                        tvAttachmentFileSize.text = size
                    } else {
                        clAttachmentFile.isVisible = false
                        clAttachmentProgress.setBackgroundResource(backgroundResource)
                        tvAttachmentProgressStatus.setTextColor(ContextCompat.getColor(this.context, textColor))
                        tvAttachmentProgress.setTextColor(ContextCompat.getColor(this.context, textColor))

                        if (item.chatMessage.outgoing) {
                            clAttachmentProgress.isVisible = false
                        } else {
                            clAttachmentProgress.isVisible = true
                            tvAttachmentProgressStatus.isVisible = true
                            tvAttachmentProgressSize.isVisible = true
                            tvAttachmentProgressSize.text = size
                            tvAttachmentProgressType.isVisible = true
                            tvAttachmentProgressType.text = this.context.getString(R.string.text_file)

                            val transferProgress = item.attachtmentTransferProgress

                            item.chatMessage.attachment?.content?.toHex()?.let { id ->
                                val stopState = transferProgress?.state == TransferState.STOPPED
                                val isStopped = parentActivity.getCommunity<PeerChatCommunity>()!!
                                    .evaProtocol!!.isStopped(Peer(item.chatMessage.sender).key, id)

                                if (stopState || isStopped) {
                                    ivAttachmentProgressPlay.isVisible = true
                                    ivAttachmentProgressStop.isVisible = false
                                } else {
                                    ivAttachmentProgressPlay.isVisible = false
                                    ivAttachmentProgressStop.isVisible = true
                                }

                                clAttachmentProgress.setOnClickListener {
                                    onProgressClick(Pair(item.chatMessage.sender, id))
                                }
                            }

                            tvAttachmentProgressStatus.text = when (transferProgress?.state) {
                                TransferState.INITIALIZING -> this.context.getString(R.string.text_state_initializing)
                                TransferState.SCHEDULED -> this.context.getString(R.string.text_state_scheduled)
                                TransferState.DOWNLOADING -> this.context.getString(R.string.text_state_downloading)
                                TransferState.STOPPED -> this.context.getString(R.string.text_state_stopped)
                                TransferState.UNKNOWN -> this.context.getString(R.string.text_state_unknown_state)
                                TransferState.FINISHED -> {
                                    clAttachmentProgress.isVisible = false
                                    this.context.getString(R.string.text_state_downloaded)
                                }
                                else -> {
                                    tvAttachmentProgressStatus.isVisible = false
                                    ""
                                }
                            }

                            pbAttachmentProgressLoadingSpinner.apply {
                                isVisible = !item.chatMessage.attachmentFetched && transferProgress != null && transferProgress.state == TransferState.DOWNLOADING
                                if (transferProgress != null) {
                                    progress = transferProgress.progress.toInt()
                                }
                            }
                            tvAttachmentProgress.isVisible =
                                !item.chatMessage.attachmentFetched
                            tvAttachmentProgress.text =
                                if (!item.chatMessage.attachmentFetched && transferProgress != null && transferProgress.state == TransferState.DOWNLOADING) {
                                    "${transferProgress.progress.toInt()}%"
                                } else ""
                        }
                    }

                    attachment.getFile(view.context).let { file ->
                        if (file.exists()) {
                            clAttachmentProgress.isVisible = false
                        }
                    }

                    clAttachmentFile.setOnClickListener {
                        onItemClick(item)
                    }
                }
                MessageAttachment.TYPE_IMAGE -> {
                    if (item.chatMessage.attachmentFetched) {
                        clAttachmentProgress.isVisible = false
                        clAttachmentPhotoVideo.isVisible = true
                        clAttachmentPhotoVideo.setBackgroundResource(backgroundResource)

                        tvAttachmentPhotoVideoSize.isVisible = item.chatMessage.attachment?.size != null
                        tvAttachmentPhotoVideoSize.setBackgroundResource(backgroundResource)
                        tvAttachmentPhotoVideoSize.setTextColor(ContextCompat.getColor(this.context, textColor))
                        tvAttachmentPhotoVideoSize.text = size
                    } else {
                        clAttachmentPhotoVideo.isVisible = false
                        clAttachmentProgress.setBackgroundResource(backgroundResource)
                        tvAttachmentProgressStatus.setTextColor(ContextCompat.getColor(this.context, textColor))
                        tvAttachmentProgress.setTextColor(ContextCompat.getColor(this.context, textColor))

                        if (item.chatMessage.outgoing) {
                            clAttachmentProgress.isVisible = false
                        } else { // if (!item.chatMessage.outgoing)
                            clAttachmentProgress.isVisible = true
                            tvAttachmentProgressStatus.isVisible = true
                            tvAttachmentProgressSize.isVisible = true
                            tvAttachmentProgressSize.text = size
                            tvAttachmentProgressType.isVisible = true
                            tvAttachmentProgressType.text = this.context.getString(R.string.text_image)

                            val transferProgress = item.attachtmentTransferProgress

                            item.chatMessage.attachment?.content?.toHex()?.let { id ->
                                val stopState = transferProgress?.state == TransferState.STOPPED
                                val isStopped = parentActivity.getCommunity<PeerChatCommunity>()!!
                                    .evaProtocol!!.isStopped(Peer(item.chatMessage.sender).key, id)

                                if (stopState || isStopped) {
                                    ivAttachmentProgressPlay.isVisible = true
                                    ivAttachmentProgressStop.isVisible = false
                                } else {
                                    ivAttachmentProgressPlay.isVisible = false
                                    ivAttachmentProgressStop.isVisible = true
                                }

                                clAttachmentProgress.setOnClickListener {
                                    onProgressClick(Pair(item.chatMessage.sender, id))
                                }
                            }

                            tvAttachmentProgressStatus.text = when (transferProgress?.state) {
                                TransferState.INITIALIZING -> this.context.getString(R.string.text_state_initializing)
                                TransferState.SCHEDULED -> this.context.getString(R.string.text_state_scheduled)
                                TransferState.DOWNLOADING -> this.context.getString(R.string.text_state_downloading)
                                TransferState.STOPPED -> this.context.getString(R.string.text_state_stopped)
                                TransferState.UNKNOWN -> this.context.getString(R.string.text_state_unknown_state)
                                TransferState.FINISHED -> {
                                    clAttachmentProgress.isVisible = false
                                    this.context.getString(R.string.text_state_downloaded)
                                }
                                else -> {
                                    tvAttachmentProgressStatus.isVisible = false
                                    ""
                                }
                            }

                            pbAttachmentProgressLoadingSpinner.apply {
                                isVisible = !item.chatMessage.attachmentFetched && transferProgress != null && transferProgress.state == TransferState.DOWNLOADING
                                if (transferProgress != null) {
                                    progress = transferProgress.progress.toInt()
                                }
                            }
                            tvAttachmentProgress.isVisible =
                                !item.chatMessage.attachmentFetched
                            tvAttachmentProgress.text =
                                if (!item.chatMessage.attachmentFetched && transferProgress != null && transferProgress.state == TransferState.DOWNLOADING) {
                                    "${transferProgress.progress.toInt()}%"
                                } else ""
                        }
                    }

                    attachment.getFile(view.context).let { file ->
                        if (file.exists()) {
                            clAttachmentProgress.isVisible = false
                            Glide.with(view).load(file).into(ivAttachmentPhotoVideo)
                            ivAttachmentPhotoVideo.clipToOutline = true
                        } else {
                            ivAttachmentPhotoVideo.setImageBitmap(null)
                        }
                    }

                    ivAttachmentPhotoVideo.setOnClickListener {
                        onItemClick(item)
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

                    generateIdenticon(
                        publicKey.substring(20, publicKey.length).toByteArray(),
                        getColorByHash(context, publicKey),
                        resources
                    ).let {
                        ivAttachmentContactIdenticon.setImageBitmap(it)
                    }

                    if (!item.chatMessage.outgoing) {
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
                    tvLocation.text = item.chatMessage.message.replace(", ", "\n")

                    clAttachmentLocation.setOnClickListener {
                        onItemClick(item)
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

                    try {
                        TransferRequest.deserialize(
                            attachment.content,
                            0
                        ).first.let { transferRequest ->
                            tvAttachmentTransferRequestTitle.text =
                                this.context.resources.getString(
                                    R.string.text_contact_chat_request_transfer,
                                    formatBalance(transferRequest.amount)
                                )

                            tvAttachmentTransferRequestDescription.isVisible =
                                transferRequest.description != ""
                            tvAttachmentTransferRequestDescription.text =
                                transferRequest.description
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        val offsetBuffer = attachment.content.copyOfRange(0, attachment.content.size)
                        JSONObject(offsetBuffer.decodeToString()).let { json ->
                            tvAttachmentTransferRequestTitle.text = this.context.resources.getString(
                                R.string.text_contact_chat_request_transfer,
                                formatBalance(json.optLong(TransferRequest.TRANSFER_REQUEST_AMOUNT))
                            )
                        }

                        item.chatMessage.message.let { description ->
                            tvAttachmentTransferRequestDescription.isVisible = description != ""
                            tvAttachmentTransferRequestDescription.text = description
                        }
                    }

                    if (!item.chatMessage.outgoing) {
                        ivAttachmentTransferRequestContinueIcon.isVisible = true

                        clAttachmentTransferRequest.setOnClickListener {
                            onItemClick(item)
                        }
                    }
                }
                MessageAttachment.TYPE_IDENTITY_UPDATED -> {
                    llChatItemTimeStatus.isVisible = false
                    clIdentityUpdated.isVisible = true
                    tvIdentityUpdatedMessage.text = item.chatMessage.message
                }
            }
        }

        // Show the transaction w/o its message
        item.chatMessage.transactionHash?.let { _ ->
            clTransaction.isVisible = true
            clTransaction.setBackgroundResource(backgroundResource)
            tvTransactionTitle.setTextColor(ContextCompat.getColor(this.context, textColor))

            if (item.chatMessage.message.isNotEmpty()) {
                tvTransactionMessage.isVisible = true
                tvTransactionMessage.text = item.chatMessage.message
                tvTransactionMessage.setTextColor(ContextCompat.getColor(this.context, textColor))
            }

            ivTransactionIconIncoming.isVisible = !item.chatMessage.outgoing
            ivTransactionIconOutgoing.isVisible = item.chatMessage.outgoing

            tvTransactionTitle.text = if (item.transaction?.transaction?.get("amount") != null) {
                val amount = formatBalance((item.transaction.transaction["amount"] as BigInteger).toLong())

                if (item.chatMessage.outgoing) {
                    ivTransactionErrorIcon.isVisible = item.blocks.find { it.linkedBlockId == item.transaction.blockId } == null
                    this.context.resources.getString(R.string.text_contact_chat_outgoing_transfer_of, amount)
                } else {
                    ivTransactionErrorIcon.isVisible = item.blocks.find { it.linkedBlockId == item.transaction.blockId && it.isAgreement } == null
                    this.context.resources.getString(R.string.text_contact_chat_incoming_transfer_of, amount)
                }
            } else {
                if (item.chatMessage.outgoing) {
                    this.context.resources.getString(R.string.text_contact_chat_outgoing_transfer_failed)
                } else {
                    ivTransactionErrorIcon.isVisible = true
                    this.context.resources.getString(R.string.text_contact_chat_incoming_transfer_unknown)
                }
            }

            clTransaction.setOnClickListener {
                onItemClick(item)
            }
        }

        // Position the message or attachments on the left or right side of the view, depending on incoming or outgoing
        val constraintSet = ConstraintSet()
        constraintSet.clone(clItem)
        constraintSet.removeFromHorizontalChain(flContent.id)
        constraintSet.removeFromHorizontalChain(llChatItemTimeStatus.id)

        constraintSet.connect(flContent.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constraintSet.connect(flContent.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

        val gravityHorizontal = if (item.chatMessage.outgoing) Gravity.END else Gravity.START
        val constraintGravity = if (item.chatMessage.outgoing) ConstraintSet.END else ConstraintSet.START

        llChatItemTimeStatus.gravity = gravityHorizontal

        flContent.setPadding(0, flContent.paddingTop, 0, flContent.paddingBottom)

        if (item.chatMessage.outgoing) {
            flContent.setPadding(100, flContent.paddingTop, flContent.paddingRight, flContent.paddingBottom)
        } else {
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
