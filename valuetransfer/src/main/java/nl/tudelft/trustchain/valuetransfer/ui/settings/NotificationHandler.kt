package nl.tudelft.trustchain.valuetransfer.ui.settings

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.ContactStore
import nl.tudelft.trustchain.common.eurotoken.TransactionRepository
import nl.tudelft.trustchain.common.util.getColorByHash
import nl.tudelft.trustchain.peerchat.entity.ChatMessage
import nl.tudelft.trustchain.peerchat.ui.conversation.MessageAttachment
import nl.tudelft.trustchain.valuetransfer.R
import nl.tudelft.trustchain.valuetransfer.ValueTransferMainActivity
import nl.tudelft.trustchain.valuetransfer.util.formatBalance
import nl.tudelft.trustchain.valuetransfer.util.generateIdenticon
import java.math.BigInteger

@Suppress("UNUSED_PARAMETER")
class NotificationHandler(
    private val parentActivity: ValueTransferMainActivity
) {
    private val contactStore: ContactStore = parentActivity.getStore()!!
    private val transactionRepository: TransactionRepository = parentActivity.getStore()!!

    val notificationManager by lazy {
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

    fun notify(peer: Peer, chatMessage: ChatMessage) {
        val notifications = NotificationManagerCompat.from(parentActivity).areNotificationsEnabled()
        val notificationsMessages = getNotificationChannelStatus(NOTIFICATION_CHANNEL_MESSAGES_ID) == NOTIFICATION_STATUS_ENABLED
        val notificationsTransactions = getNotificationChannelStatus(NOTIFICATION_CHANNEL_TRANSACTIONS_ID) == NOTIFICATION_STATUS_ENABLED

        if (notifications) {
            when {
                notificationsMessages && chatMessage.message.isNotBlank() && chatMessage.attachment == null && chatMessage.transactionHash == null -> message(
                    peer,
                    chatMessage
                )
                notificationsMessages && chatMessage.attachment != null -> chatMessage.attachment?.let { attachment ->
                    attachment(peer, attachment.type, chatMessage.message)
                }
                notificationsTransactions && chatMessage.transactionHash != null -> transaction(
                    peer,
                    chatMessage.message,
                    chatMessage.transactionHash!!
                )
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

    private fun message(peer: Peer, message: ChatMessage) {
        val intent = Intent(parentActivity, ValueTransferMainActivity::class.java)

        val extras = Bundle()
        extras.putString(ValueTransferMainActivity.ARG_FRAGMENT, ValueTransferMainActivity.contactChatFragmentTag)
        extras.putString(ValueTransferMainActivity.ARG_PUBLIC_KEY, peer.publicKey.keyToBin().toHex())
        intent.putExtras(extras)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        val pendingIntent = PendingIntent.getActivity(
            parentActivity,
            ValueTransferMainActivity.NOTIFICATION_INTENT_CHAT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        sendNotification(peer, message.message, pendingIntent, NOTIFICATION_CHANNEL_MESSAGES_ID)
    }

    private fun attachment(peer: Peer, type: String, message: String) {
        when (type) {
            MessageAttachment.TYPE_IMAGE -> "${getEmojiByUnicode(EMOJI_CAMERA)} Photo/Video"
            MessageAttachment.TYPE_CONTACT -> "${getEmojiByUnicode((EMOJI_CONTACT))} Contact"
            MessageAttachment.TYPE_LOCATION -> "${getEmojiByUnicode(EMOJI_LOCATION)} Location"
            MessageAttachment.TYPE_IDENTITY_ATTRIBUTE -> "${getEmojiByUnicode(EMOJI_IDENTITY_ATTRIBUTE)} Identity Attribute"
            MessageAttachment.TYPE_TRANSFER_REQUEST -> {
                when {
                    message != "" -> "for '$message'"
                    else -> ""
                }.let { text ->
                    "${getEmojiByUnicode(EMOJI_TRANSFER_REQUEST)} Transfer Request $text"
                }
            }
            else -> null
        }?.let { text ->
            val intent = Intent(parentActivity, ValueTransferMainActivity::class.java)
            intent.putExtra(
                ValueTransferMainActivity.ARG_FRAGMENT,
                ValueTransferMainActivity.contactChatFragmentTag
            )
            intent.putExtra(
                ValueTransferMainActivity.ARG_PUBLIC_KEY,
                peer.publicKey.keyToBin().toHex()
            )
            val pendingIntent = PendingIntent.getActivity(
                parentActivity,
                ValueTransferMainActivity.NOTIFICATION_INTENT_CHAT,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            sendNotification(peer, text, pendingIntent, NOTIFICATION_CHANNEL_MESSAGES_ID)
        }
    }

    private fun transaction(peer: Peer, message: String, transactionHash: ByteArray) {
        val transaction = transactionRepository.getTransactionWithHash(transactionHash)
        val transactionText = if (transaction != null) {
            val map = transaction.transaction.toMap()
            if (map.containsKey("amount")) {
                "${getEmojiByUnicode(EMOJI_TRANSACTION)} Incoming transaction of €${formatBalance((map["amount"] as BigInteger).toLong())}"
            } else {
                "${getEmojiByUnicode(EMOJI_TRANSACTION)} Incoming transfer"
            }
        } else {
            "${getEmojiByUnicode(EMOJI_TRANSACTION)} Incoming transfer"
        }
        val text = if (message.isNotBlank()) {
            "$transactionText for: '$message'"
        } else {
            transactionText
        }

        val intent = Intent(parentActivity, ValueTransferMainActivity::class.java)
        intent.putExtra(
            ValueTransferMainActivity.ARG_FRAGMENT,
            ValueTransferMainActivity.exchangeFragmentTag
        )
        val pendingIntent = PendingIntent.getActivity(
            parentActivity,
            ValueTransferMainActivity.NOTIFICATION_INTENT_TRANSACTION,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        sendNotification(peer, text, pendingIntent, NOTIFICATION_CHANNEL_TRANSACTIONS_ID)
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

        const val EMOJI_TRANSACTION = 0x1F4B6
        const val EMOJI_CAMERA = 0x1F4F7
        const val EMOJI_CONTACT = 0x1F464
        const val EMOJI_LOCATION = 0x1F4CD
        const val EMOJI_IDENTITY_ATTRIBUTE = 0x1F4CE
        const val EMOJI_TRANSFER_REQUEST = 0x1F4B6

        private lateinit var instance: NotificationHandler
        fun getInstance(parentActivity: ValueTransferMainActivity): NotificationHandler {
            if (!::instance.isInitialized) {
                instance = NotificationHandler(parentActivity)
            }
            return instance
        }

        private fun getEmojiByUnicode(unicode: Int): String? {
            return String(Character.toChars(unicode))
        }

        private fun getContactName(peer: Peer, contactStore: ContactStore): String {
            return contactStore.getContactFromPublicKey(peer.publicKey)?.name ?: "Unknown contact (${peer.mid})"
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
