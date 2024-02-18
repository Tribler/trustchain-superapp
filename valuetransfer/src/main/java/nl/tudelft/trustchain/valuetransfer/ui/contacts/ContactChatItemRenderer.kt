package nl.tudelft.trustchain.valuetransfer.ui.contacts

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.Glide
import com.mattskala.itemadapter.ItemLayoutRenderer
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.messaging.eva.TransferState
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.util.getColorByHash
import nl.tudelft.trustchain.common.valuetransfer.entity.IdentityAttribute
import nl.tudelft.trustchain.common.valuetransfer.entity.TransferRequest
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.community.PeerChatCommunity
import nl.tudelft.trustchain.valuetransfer.databinding.ItemContactsChatDetailBinding
import nl.tudelft.trustchain.valuetransfer.util.MessageAttachment
import nl.tudelft.trustchain.valuetransfer.util.formatBalance
import nl.tudelft.trustchain.valuetransfer.util.generateIdenticon
import nl.tudelft.trustchain.valuetransfer.util.getColorIDFromThemeAttribute
import nl.tudelft.trustchain.valuetransfer.util.getFormattedSize
import org.json.JSONObject
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        val binding = ItemContactsChatDetailBinding.bind(view)
        // Necessary to hide content types because the recycler view sometimes displays wrong data
        binding.clChatMessage.isVisible = false
        binding.clTransaction.isVisible = false
        binding.clAttachmentPhotoVideo.isVisible = false
        binding.clAttachmentFile.isVisible = false
        binding.clAttachmentIdentityAttribute.isVisible = false
        binding.clAttachmentLocation.isVisible = false
        binding.clAttachmentTransferRequest.isVisible = false
        binding.clAttachmentContact.isVisible = false
        binding.clIdentityUpdated.isVisible = false
        binding.clAttachmentProgress.isVisible = false
        binding.llChatItemTimeStatus.isVisible = true

        binding.tvChatMessage.text = ""
        binding.tvChatMessage.isVisible = false
        binding.tvTransactionMessage.text = ""
        binding.tvTransactionMessage.isVisible = false
        binding.ivTransactionErrorIcon.isVisible = false
        binding.tvAttachmentProgress.isVisible = false
        binding.tvAttachmentProgress.text = ""
        binding.tvAttachmentProgressStatus.isVisible = false
        binding.tvAttachmentProgressStatus.text = ""
        binding.ivAttachmentProgressPlay.isVisible = false
        binding.ivAttachmentProgressStop.isVisible = false
        binding.tvAttachmentProgressSize.isVisible = false
        binding.tvAttachmentProgressSize.text = ""
        binding.tvAttachmentProgressType.isVisible = false
        binding.tvAttachmentProgressType.text = ""
        binding.tvAttachmentFileName.text = ""
        binding.tvAttachmentFileName.isVisible = false
        binding.tvAttachmentFileSize.text = ""
        binding.tvAttachmentFileSize.isVisible = false
        binding.tvAttachmentTransferRequestDescription.text = ""
        binding.tvAttachmentTransferRequestDescription.isVisible = false
        binding.tvAttachmentContactName.text = ""
        binding.tvAttachmentContactName.isVisible = false
        binding.tvAttachmentContactPublicKey.text = ""
        binding.tvAttachmentContactPublicKey.isVisible = false
        binding.tvIdentityUpdatedMessage.text = ""
        binding.ivChatItemStatus.isVisible = false

        // Show the load more messages button
        binding.clLoadMoreMessagesSection.isVisible = item.loadMoreMessages
        if (item.loadMoreMessages) {
            binding.btnLoadMoreMessages.setOnClickListener {
                onLoadMoreClick(item)
            }
        }

        // Date section when item is the first of the data
        val messageDate = item.chatMessage.timestamp
        binding.clDateSection.isVisible = item.shouldShowDate
        binding.tvDateSection.text =
            if (yearFormat.format(Date()).equals(yearFormat.format(messageDate))) {
                dateFormat.format(messageDate)
            } else {
                previousYearsDateFormat.format(messageDate)
            }

        // Chat item time and status
        binding.tvChatItemTime.text = timeFormat.format(messageDate)

        if (item.chatMessage.outgoing) {
            binding.ivChatItemStatus.isVisible = item.chatMessage.outgoing

            val itemStatusResource = when {
                item.chatMessage.ack -> R.drawable.ic_check_double
                else -> R.drawable.ic_check_single
            }

            binding.ivChatItemStatus.setImageResource(itemStatusResource)
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
            binding.clChatMessage.isVisible = true
            binding.tvChatMessage.isVisible = true

            binding.clChatMessage.setBackgroundResource(backgroundResource)
            binding.tvChatMessage.setTextColor(ContextCompat.getColor(this.context, textColor))

            binding.tvChatMessage.text = item.chatMessage.message
        }

        // Show the different types of attachments
        item.chatMessage.attachment?.let { attachment ->
            val size = getFormattedSize(item.chatMessage.attachment?.size!!.toDouble())

            when (attachment.type) {
                MessageAttachment.TYPE_FILE -> {
                    if (item.chatMessage.attachmentFetched) {
                        binding.clAttachmentProgress.isVisible = false
                        binding.clAttachmentFile.isVisible = true
                        binding.clAttachmentFile.setBackgroundResource(backgroundResource)

                        binding.tvAttachmentFileName.isVisible = true
                        binding.tvAttachmentFileName.text = item.chatMessage.message
                        binding.tvAttachmentFileName.setTextColor(
                            ContextCompat.getColor(
                                this.context,
                                textColor
                            )
                        )

                        binding.tvAttachmentFileSize.isVisible =
                            item.chatMessage.attachment?.size != null
                        binding.tvAttachmentFileSize.text = size
                    } else {
                        binding.clAttachmentFile.isVisible = false
                        binding.clAttachmentProgress.setBackgroundResource(backgroundResource)
                        binding.tvAttachmentProgressStatus.setTextColor(
                            ContextCompat.getColor(
                                this.context,
                                textColor
                            )
                        )
                        binding.tvAttachmentProgress.setTextColor(
                            ContextCompat.getColor(
                                this.context,
                                textColor
                            )
                        )

                        if (item.chatMessage.outgoing) {
                            binding.clAttachmentProgress.isVisible = false
                        } else {
                            binding.clAttachmentProgress.isVisible = true
                            binding.tvAttachmentProgressStatus.isVisible = true
                            binding.tvAttachmentProgressSize.isVisible = true
                            binding.tvAttachmentProgressSize.text = size
                            binding.tvAttachmentProgressType.isVisible = true
                            binding.tvAttachmentProgressType.text =
                                this.context.getString(R.string.text_file)

                            val transferProgress = item.attachtmentTransferProgress

                            item.chatMessage.attachment?.content?.toHex()?.let { id ->
                                val stopState = transferProgress?.state == TransferState.STOPPED
                                val isStopped = parentActivity.getCommunity<PeerChatCommunity>()!!
                                    .evaProtocol!!.isStopped(Peer(item.chatMessage.sender).key, id)

                                if (stopState || isStopped) {
                                    binding.ivAttachmentProgressPlay.isVisible = true
                                    binding.ivAttachmentProgressStop.isVisible = false
                                } else {
                                    binding.ivAttachmentProgressPlay.isVisible = false
                                    binding.ivAttachmentProgressStop.isVisible = true
                                }

                                binding.clAttachmentProgress.setOnClickListener {
                                    onProgressClick(Pair(item.chatMessage.sender, id))
                                }
                            }

                            binding.tvAttachmentProgressStatus.text =
                                when (transferProgress?.state) {
                                    TransferState.INITIALIZING -> this.context.getString(R.string.text_state_initializing)
                                    TransferState.SCHEDULED -> this.context.getString(R.string.text_state_scheduled)
                                    TransferState.DOWNLOADING -> this.context.getString(R.string.text_state_downloading)
                                    TransferState.STOPPED -> this.context.getString(R.string.text_state_stopped)
                                    TransferState.UNKNOWN -> this.context.getString(R.string.text_state_unknown_state)
                                    TransferState.FINISHED -> {
                                        binding.clAttachmentProgress.isVisible = false
                                        this.context.getString(R.string.text_state_downloaded)
                                    }

                                    else -> {
                                        binding.tvAttachmentProgressStatus.isVisible = false
                                        ""
                                    }
                                }

                            binding.pbAttachmentProgressLoadingSpinner.apply {
                                isVisible =
                                    !item.chatMessage.attachmentFetched && transferProgress != null && transferProgress.state == TransferState.DOWNLOADING
                                if (transferProgress != null) {
                                    progress = transferProgress.progress.toInt()
                                }
                            }
                            binding.tvAttachmentProgress.isVisible =
                                !item.chatMessage.attachmentFetched
                            binding.tvAttachmentProgress.text =
                                if (!item.chatMessage.attachmentFetched && transferProgress != null && transferProgress.state == TransferState.DOWNLOADING) {
                                    "${transferProgress.progress.toInt()}%"
                                } else ""
                        }
                    }

                    attachment.getFile(view.context).let { file ->
                        if (file.exists()) {
                            binding.clAttachmentProgress.isVisible = false
                        }
                    }

                    binding.clAttachmentFile.setOnClickListener {
                        onItemClick(item)
                    }
                }

                MessageAttachment.TYPE_IMAGE -> {
                    if (item.chatMessage.attachmentFetched) {
                        binding.clAttachmentProgress.isVisible = false
                        binding.clAttachmentPhotoVideo.isVisible = true
                        binding.clAttachmentPhotoVideo.setBackgroundResource(backgroundResource)

                        binding.tvAttachmentPhotoVideoSize.isVisible =
                            item.chatMessage.attachment?.size != null
                        binding.tvAttachmentPhotoVideoSize.setBackgroundResource(backgroundResource)
                        binding.tvAttachmentPhotoVideoSize.setTextColor(
                            ContextCompat.getColor(
                                this.context,
                                textColor
                            )
                        )
                        binding.tvAttachmentPhotoVideoSize.text = size
                    } else {
                        binding.clAttachmentPhotoVideo.isVisible = false
                        binding.clAttachmentProgress.setBackgroundResource(backgroundResource)
                        binding.tvAttachmentProgressStatus.setTextColor(
                            ContextCompat.getColor(
                                this.context,
                                textColor
                            )
                        )
                        binding.tvAttachmentProgress.setTextColor(
                            ContextCompat.getColor(
                                this.context,
                                textColor
                            )
                        )

                        if (item.chatMessage.outgoing) {
                            binding.clAttachmentProgress.isVisible = false
                        } else { // if (!item.chatMessage.outgoing)
                            binding.clAttachmentProgress.isVisible = true
                            binding.tvAttachmentProgressStatus.isVisible = true
                            binding.tvAttachmentProgressSize.isVisible = true
                            binding.tvAttachmentProgressSize.text = size
                            binding.tvAttachmentProgressType.isVisible = true
                            binding.tvAttachmentProgressType.text =
                                this.context.getString(R.string.text_image)

                            val transferProgress = item.attachtmentTransferProgress

                            item.chatMessage.attachment?.content?.toHex()?.let { id ->
                                val stopState = transferProgress?.state == TransferState.STOPPED
                                val isStopped = parentActivity.getCommunity<PeerChatCommunity>()!!
                                    .evaProtocol!!.isStopped(Peer(item.chatMessage.sender).key, id)

                                if (stopState || isStopped) {
                                    binding.ivAttachmentProgressPlay.isVisible = true
                                    binding.ivAttachmentProgressStop.isVisible = false
                                } else {
                                    binding.ivAttachmentProgressPlay.isVisible = false
                                    binding.ivAttachmentProgressStop.isVisible = true
                                }

                                binding.clAttachmentProgress.setOnClickListener {
                                    onProgressClick(Pair(item.chatMessage.sender, id))
                                }
                            }

                            binding.tvAttachmentProgressStatus.text =
                                when (transferProgress?.state) {
                                    TransferState.INITIALIZING -> this.context.getString(R.string.text_state_initializing)
                                    TransferState.SCHEDULED -> this.context.getString(R.string.text_state_scheduled)
                                    TransferState.DOWNLOADING -> this.context.getString(R.string.text_state_downloading)
                                    TransferState.STOPPED -> this.context.getString(R.string.text_state_stopped)
                                    TransferState.UNKNOWN -> this.context.getString(R.string.text_state_unknown_state)
                                    TransferState.FINISHED -> {
                                        binding.clAttachmentProgress.isVisible = false
                                        this.context.getString(R.string.text_state_downloaded)
                                    }

                                    else -> {
                                        binding.tvAttachmentProgressStatus.isVisible = false
                                        ""
                                    }
                                }

                            binding.pbAttachmentProgressLoadingSpinner.apply {
                                isVisible =
                                    !item.chatMessage.attachmentFetched && transferProgress != null && transferProgress.state == TransferState.DOWNLOADING
                                if (transferProgress != null) {
                                    progress = transferProgress.progress.toInt()
                                }
                            }
                            binding.tvAttachmentProgress.isVisible =
                                !item.chatMessage.attachmentFetched
                            binding.tvAttachmentProgress.text =
                                if (!item.chatMessage.attachmentFetched && transferProgress != null && transferProgress.state == TransferState.DOWNLOADING) {
                                    "${transferProgress.progress.toInt()}%"
                                } else ""
                        }
                    }

                    attachment.getFile(view.context).let { file ->
                        if (file.exists()) {
                            binding.clAttachmentProgress.isVisible = false
                            Glide.with(view).load(file).into(binding.ivAttachmentPhotoVideo)
                            binding.ivAttachmentPhotoVideo.clipToOutline = true
                        } else {
                            binding.ivAttachmentPhotoVideo.setImageBitmap(null)
                        }
                    }

                    binding.ivAttachmentPhotoVideo.setOnClickListener {
                        onItemClick(item)
                    }
                }

                MessageAttachment.TYPE_CONTACT -> {
                    binding.clAttachmentContact.isVisible = true
                    binding.tvAttachmentContactName.isVisible = true
                    binding.tvAttachmentContactPublicKey.isVisible = true

                    binding.clAttachmentContact.setBackgroundResource(backgroundResource)
                    binding.tvAttachmentContactName.setTextColor(
                        ContextCompat.getColor(
                            this.context,
                            textColor
                        )
                    )
                    binding.tvAttachmentContactPublicKey.setTextColor(
                        ContextCompat.getColor(
                            this.context,
                            textColor
                        )
                    )

                    val contact = Contact.deserialize(attachment.content, 0).first
                    binding.tvAttachmentContactName.text = contact.name
                    val publicKey = contact.publicKey.keyToBin().toHex()
                    binding.tvAttachmentContactPublicKey.text = publicKey

                    generateIdenticon(
                        publicKey.substring(20, publicKey.length).toByteArray(),
                        getColorByHash(context, publicKey),
                        resources
                    ).let {
                        binding.ivAttachmentContactIdenticon.setImageBitmap(it)
                    }

                    if (!item.chatMessage.outgoing) {
                        binding.clAttachmentContactIcon.isVisible = true
                        binding.clAttachmentContact.setOnClickListener {
                            onItemClick(item)
                        }
                    }
                }

                MessageAttachment.TYPE_LOCATION -> {
                    binding.clAttachmentLocation.isVisible = true

                    binding.clAttachmentLocation.setBackgroundResource(backgroundResource)
                    binding.tvLocation.setTextColor(ContextCompat.getColor(this.context, textColor))
                    binding.tvLocation.text = item.chatMessage.message.replace(", ", "\n")

                    binding.clAttachmentLocation.setOnClickListener {
                        onItemClick(item)
                    }
                }

                MessageAttachment.TYPE_IDENTITY_ATTRIBUTE -> {
                    binding.clAttachmentIdentityAttribute.isVisible = true

                    binding.clAttachmentIdentityAttribute.setBackgroundResource(backgroundResource)
                    binding.tvIdentityAttributeName.setTextColor(
                        ContextCompat.getColor(
                            this.context,
                            textColor
                        )
                    )
                    binding.tvIdentityAttributeValue.setTextColor(
                        ContextCompat.getColor(
                            this.context,
                            textColor
                        )
                    )

                    val identityAttribute =
                        IdentityAttribute.deserialize(attachment.content, 0).first
                    binding.tvIdentityAttributeName.text = identityAttribute.name
                    binding.tvIdentityAttributeValue.text = identityAttribute.value

                    if (!item.chatMessage.outgoing) {
                        binding.clAttachmentIdentityAttribute.setOnClickListener {
                            onItemClick(item)
                        }
                    }
                }

                MessageAttachment.TYPE_TRANSFER_REQUEST -> {
                    binding.clAttachmentTransferRequest.isVisible = true

                    binding.clAttachmentTransferRequest.setBackgroundResource(backgroundResource)
                    binding.tvAttachmentTransferRequestTitle.setTextColor(
                        ContextCompat.getColor(
                            this.context,
                            textColor
                        )
                    )
                    binding.tvAttachmentTransferRequestDescription.setTextColor(
                        ContextCompat.getColor(
                            this.context,
                            textColor
                        )
                    )

                    try {
                        TransferRequest.deserialize(
                            attachment.content,
                            0
                        ).first.let { transferRequest ->
                            binding.tvAttachmentTransferRequestTitle.text =
                                this.context.resources.getString(
                                    R.string.text_contact_chat_request_transfer,
                                    formatBalance(transferRequest.amount)
                                )

                            binding.tvAttachmentTransferRequestDescription.isVisible =
                                transferRequest.description != ""
                            binding.tvAttachmentTransferRequestDescription.text =
                                transferRequest.description
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        val offsetBuffer =
                            attachment.content.copyOfRange(0, attachment.content.size)
                        JSONObject(offsetBuffer.decodeToString()).let { json ->
                            binding.tvAttachmentTransferRequestTitle.text =
                                this.context.resources.getString(
                                    R.string.text_contact_chat_request_transfer,
                                    formatBalance(json.optLong(TransferRequest.TRANSFER_REQUEST_AMOUNT))
                                )
                        }

                        item.chatMessage.message.let { description ->
                            binding.tvAttachmentTransferRequestDescription.isVisible =
                                description != ""
                            binding.tvAttachmentTransferRequestDescription.text = description
                        }
                    }

                    if (!item.chatMessage.outgoing) {
                        binding.ivAttachmentTransferRequestContinueIcon.isVisible = true

                        binding.clAttachmentTransferRequest.setOnClickListener {
                            onItemClick(item)
                        }
                    }
                }

                MessageAttachment.TYPE_IDENTITY_UPDATED -> {
                    binding.llChatItemTimeStatus.isVisible = false
                    binding.clIdentityUpdated.isVisible = true
                    binding.tvIdentityUpdatedMessage.text = item.chatMessage.message
                }
            }
        }

        // Show the transaction w/o its message
        item.chatMessage.transactionHash?.let { _ ->
            binding.clTransaction.isVisible = true
            binding.clTransaction.setBackgroundResource(backgroundResource)
            binding.tvTransactionTitle.setTextColor(ContextCompat.getColor(this.context, textColor))

            if (item.chatMessage.message.isNotEmpty()) {
                binding.tvTransactionMessage.isVisible = true
                binding.tvTransactionMessage.text = item.chatMessage.message
                binding.tvTransactionMessage.setTextColor(
                    ContextCompat.getColor(
                        this.context,
                        textColor
                    )
                )
            }

            binding.ivTransactionIconIncoming.isVisible = !item.chatMessage.outgoing
            binding.ivTransactionIconOutgoing.isVisible = item.chatMessage.outgoing

            binding.tvTransactionTitle.text =
                if (item.transaction?.transaction?.get("amount") != null) {
                    val amount =
                        formatBalance((item.transaction.transaction["amount"] as BigInteger).toLong())

                    if (item.chatMessage.outgoing) {
                        binding.ivTransactionErrorIcon.isVisible =
                            item.blocks.find { it.linkedBlockId == item.transaction.blockId } == null
                        this.context.resources.getString(
                            R.string.text_contact_chat_outgoing_transfer_of,
                            amount
                        )
                    } else {
                        binding.ivTransactionErrorIcon.isVisible =
                            item.blocks.find { it.linkedBlockId == item.transaction.blockId && it.isAgreement } == null
                        this.context.resources.getString(
                            R.string.text_contact_chat_incoming_transfer_of,
                            amount
                        )
                    }
                } else {
                    if (item.chatMessage.outgoing) {
                        this.context.resources.getString(R.string.text_contact_chat_outgoing_transfer_failed)
                    } else {
                        binding.ivTransactionErrorIcon.isVisible = true
                        this.context.resources.getString(R.string.text_contact_chat_incoming_transfer_unknown)
                    }
                }

            binding.clTransaction.setOnClickListener {
                onItemClick(item)
            }
        }

        // Position the message or attachments on the left or right side of the view, depending on incoming or outgoing
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.clItem)
        constraintSet.removeFromHorizontalChain(binding.flContent.id)
        constraintSet.removeFromHorizontalChain(binding.llChatItemTimeStatus.id)

        constraintSet.connect(
            binding.flContent.id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START
        )
        constraintSet.connect(
            binding.flContent.id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END
        )

        val gravityHorizontal = if (item.chatMessage.outgoing) Gravity.END else Gravity.START
        val constraintGravity =
            if (item.chatMessage.outgoing) ConstraintSet.END else ConstraintSet.START

        binding.llChatItemTimeStatus.gravity = gravityHorizontal

        binding.flContent.setPadding(
            0,
            binding.flContent.paddingTop,
            0,
            binding.flContent.paddingBottom
        )

        if (item.chatMessage.outgoing) {
            binding.flContent.setPadding(
                100,
                binding.flContent.paddingTop,
                binding.flContent.paddingRight,
                binding.flContent.paddingBottom
            )
        } else {
            binding.flContent.setPadding(
                binding.flContent.paddingLeft,
                binding.flContent.paddingTop,
                100,
                binding.flContent.paddingBottom
            )
        }

        binding.flChatItemContent.updateLayoutParams<FrameLayout.LayoutParams> {
            gravity = gravityHorizontal
        }

        constraintSet.connect(
            binding.llChatItemTimeStatus.id,
            constraintGravity,
            ConstraintSet.PARENT_ID,
            constraintGravity
        )
        constraintSet.applyTo(binding.clItem)
    }

    override fun getLayoutResourceId(): Int {
        return R.layout.item_contacts_chat_detail
    }
}
