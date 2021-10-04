package nl.tudelft.trustchain.valuetransfer.ui.settings

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.util.getColorByHash
import nl.tudelft.trustchain.peerchat.db.PeerChatStore
import nl.tudelft.trustchain.peerchat.entity.ChatMessage
import nl.tudelft.trustchain.peerchat.ui.conversation.MessageAttachment
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.ui.QRScanController
import nl.tudelft.trustchain.valuetransfer.util.formatBalance
import nl.tudelft.trustchain.valuetransfer.util.generateIdenticon
import java.math.BigInteger

@Suppress("UNUSED_PARAMETER")
class NotificationHandler(
    private val parentActivity: ValueTransferMainActivity
) {
    private val peerChatStore: PeerChatStore = parentActivity.getStore()!!
    private val contactStore: ContactStore = parentActivity.getStore()!!
    private val transactionRepository: TransactionRepository = parentActivity.getStore()!!

    private val notificationManager by lazy {
        parentActivity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    private lateinit var messagesChannel: NotificationChannel
    private lateinit var transactionsChannel: NotificationChannel
    private var notificationCount = 0

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            messagesChannel = if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_MESSAGES_ID) == null) {
                NotificationChannel(
                    NOTIFICATION_CHANNEL_MESSAGES_ID,
                    NOTIFICATION_CHANNEL_MESSAGES_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = NOTIFICATION_CHANNEL_MESSAGES_DESCRIPTION
                    notificationManager.createNotificationChannel(this)
                }
             } else {
                notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_MESSAGES_ID)
            }

            transactionsChannel = if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_TRANSACTIONS_ID) == null) {
                NotificationChannel(
                    NOTIFICATION_CHANNEL_TRANSACTIONS_ID,
                    NOTIFICATION_CHANNEL_TRANSACTIONS_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = NOTIFICATION_CHANNEL_TRANSACTIONS_DESCRIPTION
                    notificationManager.createNotificationChannel(this)
                }
            } else {
                notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_TRANSACTIONS_ID)
            }
        }
    }

    fun notify(peer: Peer, chatMessage: ChatMessage) {
        if (NotificationManagerCompat.from(parentActivity).areNotificationsEnabled()) {
            when (typeOfMessage(chatMessage)) {
                TYPE_MESSAGE -> message(peer, chatMessage)
                TYPE_ATTACHMENT -> chatMessage.attachment?.let { attachment ->
                    attachment(peer, attachment.type, chatMessage.message)
                }
                TYPE_TRANSACTION -> transaction(peer, chatMessage.message, chatMessage.transactionHash!!)
            }
        }
    }

    private fun sendNotification(peer: Peer, text: String, intent: PendingIntent, channelID: String) {
        val builder = NotificationCompat.Builder(parentActivity, channelID)
            .setSmallIcon(R.drawable.ic_blockchain_identity)
            .setContentTitle(getContactName(peer, contactStore))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(text)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(intent)
            .setGroup(NOTIFICATION_GROUP_ID)
            .setGroupSummary(true)

        getIcon(peer, parentActivity).let { icon ->
            builder.setLargeIcon(icon)
        }

        with(NotificationManagerCompat.from(parentActivity)) {
            notify(notificationCount, builder.build())
        }

        notificationCount++
    }

    fun getNotificationChannelStatus(channelID: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return if (notificationManager.getNotificationChannel(channelID).importance != NotificationManager.IMPORTANCE_NONE) {
                NOTIFICATION_STATUS_ENABLED
            } else {
                NOTIFICATION_STATUS_DISABLED
            }
        }
        return NOTIFICATION_STATUS_UNKNOWN
    }

    fun typeOfMessage(chatMessage: ChatMessage): String {
        val notificationsMessages = getNotificationChannelStatus(NOTIFICATION_CHANNEL_MESSAGES_ID) == NOTIFICATION_STATUS_ENABLED
        val notificationsTransactions = getNotificationChannelStatus(NOTIFICATION_CHANNEL_TRANSACTIONS_ID) == NOTIFICATION_STATUS_ENABLED

        return when {
            notificationsMessages && chatMessage.message.isNotBlank() && chatMessage.attachment == null && chatMessage.transactionHash == null -> TYPE_MESSAGE
            notificationsMessages && chatMessage.attachment != null -> TYPE_ATTACHMENT
            notificationsTransactions && chatMessage.transactionHash != null -> TYPE_TRANSACTION
            else -> ""
        }
    }

    private fun message(peer: Peer, chatMessage: ChatMessage) {
        val intent = Intent(parentActivity, ValueTransferMainActivity::class.java).apply {
            putExtra(
                ValueTransferMainActivity.ARG_FRAGMENT,
                ValueTransferMainActivity.contactChatFragmentTag
            )
            putExtra(
                ValueTransferMainActivity.ARG_PUBLIC_KEY,
                peer.publicKey.keyToBin().toHex()
            )
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        val pendingIntent = PendingIntent.getActivity(
            parentActivity,
            ValueTransferMainActivity.NOTIFICATION_INTENT_CHAT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notAContact = contactStore.getContactFromPublicKey(peer.publicKey) == null
        val isFirstMessage = peerChatStore.getAllSentByPublicKeyToMe(peer.publicKey).size <= 1

        val message = if (notAContact && isFirstMessage) {
            parentActivity.resources.getString(R.string.text_contact_chat_request_to_chat)
        } else {
            chatMessage.message
        }

        sendNotification(peer, message, pendingIntent, NOTIFICATION_CHANNEL_MESSAGES_ID)
    }

    private fun attachment(peer: Peer, type: String, message: String) {
        when (type) {
            MessageAttachment.TYPE_IMAGE -> StringBuilder()
                .append(getEmojiByUnicode(EMOJI_CAMERA))
                .append(" ")
                .append(ATTACHMENT_TYPE_PHOTO_VIDEO)
            MessageAttachment.TYPE_CONTACT -> StringBuilder()
                .append(getEmojiByUnicode(EMOJI_CONTACT))
                .append(" ")
                .append(ATTACHMENT_TYPE_CONTACT)
            MessageAttachment.TYPE_LOCATION -> StringBuilder()
                .append(getEmojiByUnicode(EMOJI_LOCATION))
                .append(" ")
                .append(ATTACHMENT_TYPE_LOCATION)
            MessageAttachment.TYPE_IDENTITY_ATTRIBUTE -> StringBuilder()
                .append(getEmojiByUnicode(EMOJI_IDENTITY_ATTRIBUTE))
                .append(" ")
                .append(ATTACHMENT_TYPE_IDENTITY_ATTRIBUTE)
            MessageAttachment.TYPE_TRANSFER_REQUEST -> {
                when {
                    message != "" -> parentActivity.resources.getString(
                        R.string.text_contact_chat_incoming_transfer_request_with_message,
                        message
                    )
                    else -> ""
                }.let { text ->
                    StringBuilder()
                        .append(getEmojiByUnicode(EMOJI_TRANSFER_REQUEST))
                        .append(" ")
                        .append(ATTACHMENT_TYPE_TRANSFER_REQUEST)
                        .append(" ")
                        .append(text)
                }
            }
            else -> null
        }?.let { text ->
            val intent = Intent(parentActivity, ValueTransferMainActivity::class.java).apply {
                putExtra(
                    ValueTransferMainActivity.ARG_FRAGMENT,
                    ValueTransferMainActivity.contactChatFragmentTag
                )
                putExtra(
                    ValueTransferMainActivity.ARG_PUBLIC_KEY,
                    peer.publicKey.keyToBin().toHex()
                )
            }
            val pendingIntent = PendingIntent.getActivity(
                parentActivity,
                ValueTransferMainActivity.NOTIFICATION_INTENT_CHAT,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            sendNotification(
                peer,
                text.toString(),
                pendingIntent,
                NOTIFICATION_CHANNEL_MESSAGES_ID
            )
        }
    }

    private fun transaction(peer: Peer, message: String, transactionHash: ByteArray) {
        val transaction = transactionRepository.getTransactionWithHash(transactionHash)
        val transactionText = if (transaction != null) {
            val map = transaction.transaction.toMap()
            if (map.containsKey(QRScanController.KEY_AMOUNT)) {
                StringBuilder()
                    .append(getEmojiByUnicode(EMOJI_TRANSACTION))
                    .append(" ")
                    .append(parentActivity.resources.getString(
                        R.string.text_contact_chat_incoming_transfer_of,
                        formatBalance((map[QRScanController.KEY_AMOUNT] as BigInteger).toLong())
                    ))
            } else {
                StringBuilder()
                    .append(getEmojiByUnicode(EMOJI_TRANSACTION))
                    .append(" ")
                    .append(parentActivity.resources.getString(R.string.text_contact_chat_incoming_transfer))
            }
        } else {
            StringBuilder()
                .append(getEmojiByUnicode(EMOJI_TRANSACTION))
                .append(" ")
                .append(parentActivity.resources.getString(R.string.text_contact_chat_incoming_transfer))
        }
        val text = if (message.isNotBlank()) {
            parentActivity.getString(
                R.string.text_contact_chat_incoming_transfer_with_message,
                transactionText,
                message
            )
        } else {
            transactionText
        }

        val intent = Intent(parentActivity, ValueTransferMainActivity::class.java).apply {
            putExtra(
                ValueTransferMainActivity.ARG_FRAGMENT,
                ValueTransferMainActivity.exchangeFragmentTag
            )
        }
        val pendingIntent = PendingIntent.getActivity(
            parentActivity,
            ValueTransferMainActivity.NOTIFICATION_INTENT_TRANSACTION,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        sendNotification(
            peer,
            text.toString(),
            pendingIntent,
            NOTIFICATION_CHANNEL_TRANSACTIONS_ID
        )
    }

    companion object {
        const val NOTIFICATION_CHANNEL_MESSAGES_ID = "vt_notification_channel_messages"
        const val NOTIFICATION_CHANNEL_MESSAGES_NAME = "Messages"
        const val NOTIFICATION_CHANNEL_MESSAGES_DESCRIPTION = "Notifications of incoming messages, attachments, and friend requests"
        const val NOTIFICATION_CHANNEL_TRANSACTIONS_ID = "vt_notification_channel_transactions"
        const val NOTIFICATION_CHANNEL_TRANSACTIONS_NAME = "Transactions"
        const val NOTIFICATION_CHANNEL_TRANSACTIONS_DESCRIPTION = "Notifications of incoming transactions"
        const val NOTIFICATION_GROUP_ID = "vt_notification_group"
        const val NOTIFICATION_STATUS_ENABLED = "ENABLED"
        const val NOTIFICATION_STATUS_DISABLED = "DISABLED"
        const val NOTIFICATION_STATUS_UNKNOWN = "UNKNOWN"

        const val TYPE_MESSAGE = "type_message"
        const val TYPE_ATTACHMENT = "type_attachment"
        const val TYPE_TRANSACTION = "type_transaction"

        const val EMOJI_TRANSACTION = 0x1F4B6
        const val EMOJI_CAMERA = 0x1F4F7
        const val EMOJI_CONTACT = 0x1F464
        const val EMOJI_LOCATION = 0x1F4CD
        const val EMOJI_IDENTITY_ATTRIBUTE = 0x1F4CE
        const val EMOJI_TRANSFER_REQUEST = 0x1F4B6

        const val ATTACHMENT_TYPE_PHOTO_VIDEO = "Photo/Video"
        const val ATTACHMENT_TYPE_IDENTITY_ATTRIBUTE = "Identity Attribute"
        const val ATTACHMENT_TYPE_CONTACT = "Contact"
        const val ATTACHMENT_TYPE_LOCATION = "Location"
        const val ATTACHMENT_TYPE_TRANSFER_REQUEST = "Transfer Request"

        private lateinit var instance: NotificationHandler
        fun getInstance(parentActivity: ValueTransferMainActivity): NotificationHandler {
            if (!::instance.isInitialized) {
                instance = NotificationHandler(parentActivity)
            }
            return instance
        }

        private fun getEmojiByUnicode(unicode: Int): String {
            return String(Character.toChars(unicode))
        }

        private fun getContactName(peer: Peer, contactStore: ContactStore): String {
            return contactStore.getContactFromPublicKey(peer.publicKey)?.name ?: instance.parentActivity.resources.getString(
                R.string.text_unknown_contact_extra,
                peer.mid
            )
        }

        private fun getIcon(peer: Peer, activity: ValueTransferMainActivity): Bitmap {
            val publicKeyString = peer.publicKey.keyToBin().toHex()

            return generateIdenticon(
                publicKeyString.substring(20, publicKeyString.length).toByteArray(),
                getColorByHash(activity, publicKeyString),
                activity.resources
            )
        }
    }
}
