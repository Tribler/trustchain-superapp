package nl.tudelft.trustchain.peerchat.community

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.peerchat.db.PeerChatStore
import nl.tudelft.trustchain.peerchat.entity.ChatMessage
import nl.tudelft.trustchain.peerchat.ui.conversation.MessageAttachment
import java.io.File
import java.io.FileOutputStream
import java.util.*

private val logger = KotlinLogging.logger {}

class PeerChatCommunity(
    private val database: PeerChatStore,
    private val context: Context
) : Community() {
    override val serviceId = "ac9c01202e8d01e5f7d3cec88085dd842267c273"

    init {
        messageHandlers[MessageId.MESSAGE] = ::onMessagePacket
        messageHandlers[MessageId.ACK] = ::onAckPacket
        messageHandlers[MessageId.ATTACHMENT_REQUEST] = ::onAttachmentRequestPacket
        messageHandlers[MessageId.ATTACHMENT] = ::onAttachmentPacket
    }

    override fun load() {
        super.load()

        scope.launch {
            while (isActive) {
                try {
                    // Send unacknowledged messages
                    val messages = database.getUnacknowledgedMessages()
                    Log.d("PeerChat", "Found ${messages.size} outgoing unack messages")
                    messages.forEach { message ->
                        sendMessage(message)
                    }

                    // Request missing attachments
                    val attachments = database.getUnfetchedAttachments()
                    Log.d("PeerChat", "Found ${attachments.size} unfetched attachments")
                    attachments.forEach { message ->
                        val mid = message.sender.keyToHash().toHex()
                        val peer = getPeers().find { it.mid == mid }
                        if (peer != null && message.attachment != null) {
                            sendAttachmentRequest(peer, message.attachment.content.toHex())
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(30000L)
            }
        }
    }

    fun sendMessageWithTransaction(
        message: String,
        transaction_hash: ByteArray?,
        recipient: PublicKey
    ) {
        val chatMessage = createOutgoingChatMessage(message, null, transaction_hash, recipient)
        database.addMessage(chatMessage)
        sendMessage(chatMessage)
    }

    fun sendMessage(message: String, recipient: PublicKey) {
        val chatMessage = createOutgoingChatMessage(message, null, null, recipient)
        database.addMessage(chatMessage)

        sendMessage(chatMessage)
    }

    fun sendImage(file: File, recipient: PublicKey) {
        val hash = file.name.hexToBytes()
        val attachment = MessageAttachment(MessageAttachment.TYPE_IMAGE, file.length(), hash)
        val chatMessage = createOutgoingChatMessage("", attachment, null, recipient)
        database.addMessage(chatMessage)

        sendMessage(chatMessage)
    }

    fun sendContact(contact: Contact, recipient: PublicKey) {
        val serializedContact = contact.serialize()
        val attachment = MessageAttachment(MessageAttachment.TYPE_CONTACT,
            serializedContact.size.toLong(), serializedContact)
        val chatMessage = createOutgoingChatMessage(contact.name, attachment, null, recipient)

        database.addMessage(chatMessage)
        sendMessage(chatMessage)
    }

    private fun sendMessage(chatMessage: ChatMessage) {
        val mid = chatMessage.recipient.keyToHash().toHex()
        val peer = getPeers().find { it.mid == mid }

        if (peer != null) {
            val payload = MessagePayload(
                chatMessage.id,
                chatMessage.message,
                chatMessage.attachment?.type ?: "",
                chatMessage.attachment?.size ?: 0L,
                chatMessage.attachment?.content ?: ByteArray(0),
                chatMessage.transactionHash
            )

            val packet = serializePacket(
                MessageId.MESSAGE,
                payload,
                sign = true,
                encrypt = true,
                recipient = peer
            )

            logger.debug { "-> $payload, ${chatMessage.transactionHash}, ${payload.transactionHash}" }
            send(peer, packet)
        } else {
            Log.d("PeerChat", "Peer $mid not online")
        }
    }

    private fun sendAck(peer: Peer, id: String) {
        val payload = AckPayload(id)
        val packet = serializePacket(MessageId.ACK, payload)
        logger.debug { "-> $payload" }
        send(peer, packet)
    }

    private fun sendAttachmentRequest(peer: Peer, id: String) {
        val payload = AttachmentRequestPayload(id)
        val packet = serializePacket(MessageId.ATTACHMENT_REQUEST, payload)
        logger.debug { "-> $payload" }
        send(peer, packet)
    }

    private fun sendAttachment(peer: Peer, id: String, file: File) {
        val payload = AttachmentPayload(id, file.readBytes())
        val packet =
            serializePacket(MessageId.ATTACHMENT, payload, encrypt = true, recipient = peer)
        logger.debug { "-> $payload" }
        send(peer, packet)
    }

    private fun onMessagePacket(packet: Packet) {
        val (peer, payload) = packet.getDecryptedAuthPayload(
            MessagePayload.Deserializer, myPeer.key as PrivateKey
        )
        logger.debug { "<- $payload, ${payload.transactionHash}" }
        onMessage(peer, payload)
    }

    private fun onAckPacket(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(AckPayload.Deserializer)
        logger.debug { "<- $payload" }
        onAck(peer, payload)
    }

    private fun onAttachmentRequestPacket(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(AttachmentRequestPayload.Deserializer)
        logger.debug { "<- $payload" }
        onAttachmentRequest(peer, payload)
    }

    private fun onAttachmentPacket(packet: Packet) {
        val (_, payload) = packet.getDecryptedAuthPayload(
            AttachmentPayload.Deserializer, myPeer.key as PrivateKey
        )
        logger.debug { "<- $payload" }
        onAttachment(payload)
    }

    private fun onMessage(peer: Peer, payload: MessagePayload) {
        Log.d("PeerChat", "onMessage from $peer: $payload")

        val chatMessage = createIncomingChatMessage(peer, payload)
        try {
            database.addMessage(chatMessage)
        } catch (e: SQLiteConstraintException) {
            e.printStackTrace()
        }

        Log.d("PeerChat", "Sending ack to ${chatMessage.id}")
        sendAck(peer, chatMessage.id)

        if (chatMessage.attachment != null) {
            when (chatMessage.attachment.type) {
                MessageAttachment.TYPE_CONTACT -> return
                else -> {
                    // Request attachment
                    sendAttachmentRequest(peer, chatMessage.attachment.content.toHex())
                }
            }
        }
    }

    private fun onAck(peer: Peer, payload: AckPayload) {
        Log.d("PeerChat", "onAck from $peer: $payload")

        try {
            database.ackMessage(payload.id)
        } catch (e: SQLiteConstraintException) {
            e.printStackTrace()
        }
    }

    private fun onAttachmentRequest(peer: Peer, payload: AttachmentRequestPayload) {
        try {
            val file = MessageAttachment.getFile(context, payload.id)
            if (file.exists()) {
                sendAttachment(peer, payload.id, file)
            } else {
                Log.w("PeerChat", "The requested attachment does not exist")
            }
        } catch (e: SQLiteConstraintException) {
            e.printStackTrace()
        }
    }

    private fun onAttachment(payload: AttachmentPayload) {
        try {
            val file = MessageAttachment.getFile(context, payload.id)
            if (!file.exists()) {
                // Save attachment
                val os = FileOutputStream(file)
                os.write(payload.data)
            }
            // Mark attachment as fetched
            database.setAttachmentFetched(payload.id)
        } catch (e: SQLiteConstraintException) {
            e.printStackTrace()
        }
    }

    private fun createOutgoingChatMessage(
        message: String,
        attachment: MessageAttachment?,
        transaction_hash: ByteArray?,
        recipient: PublicKey
    ): ChatMessage {
        val id = UUID.randomUUID().toString()
        return ChatMessage(
            id,
            message,
            attachment,
            myPeer.publicKey,
            recipient,
            true,
            Date(),
            ack = false,
            read = true,
            attachmentFetched = true,
            transactionHash = transaction_hash
        )
    }

    private fun createIncomingChatMessage(peer: Peer, message: MessagePayload): ChatMessage {
        /*
        // Store attachment
        val file = if (message.attachmentData.isNotEmpty())
            saveFile(context, message.attachmentData) else null
         */

        logger.debug { "got message with hash ${message.transactionHash}" }

        return ChatMessage(
            message.id,
            message.message,
            createMessageAttachment(message),
            peer.publicKey,
            myPeer.publicKey,
            false,
            Date(),
            ack = false,
            read = false,
            attachmentFetched = false,
            transactionHash = message.transactionHash
        )
    }

    private fun createMessageAttachment(message: MessagePayload): MessageAttachment? {
        return if (message.attachmentType.isNotEmpty()) {
            MessageAttachment(
                message.attachmentType,
                message.attachmentSize,
                message.attachmentContent
            )
        } else {
            null
        }
    }

    object MessageId {
        const val MESSAGE = 1
        const val ACK = 2
        const val ATTACHMENT_REQUEST = 3
        const val ATTACHMENT = 4
    }

    class Factory(
        private val database: PeerChatStore,
        private val context: Context
    ) : Overlay.Factory<PeerChatCommunity>(PeerChatCommunity::class.java) {
        override fun create(): PeerChatCommunity {
            return PeerChatCommunity(database, context)
        }
    }
}
