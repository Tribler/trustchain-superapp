package nl.tudelft.trustchain.peerchat.community

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.location.Location
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
import nl.tudelft.ipv8.messaging.eva.*
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.common.contacts.Contact
import nl.tudelft.trustchain.common.valuetransfer.entity.IdentityAttribute
import nl.tudelft.trustchain.common.valuetransfer.entity.IdentityInfo
import nl.tudelft.trustchain.common.valuetransfer.entity.TransferRequest
import nl.tudelft.trustchain.peerchat.db.PeerChatStore
import nl.tudelft.trustchain.peerchat.entity.ChatMessage
import nl.tudelft.trustchain.peerchat.entity.ContactImage
import nl.tudelft.trustchain.peerchat.ui.conversation.MessageAttachment
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.*

private val logger = KotlinLogging.logger {}

@OptIn(ExperimentalUnsignedTypes::class)
class PeerChatCommunity(
    private val database: PeerChatStore,
    private val context: Context
) : Community() {
    override val serviceId = "ac9c01202e8d01e5f7d3cec88085dd842267c273"

    private lateinit var onMessageCallback: (instance: PeerChatCommunity, peer: Peer, chatMessage: ChatMessage) -> Unit
    private lateinit var onContactImageRequestCallback: (instance: PeerChatCommunity, peer: Peer) -> Unit
    private lateinit var onContactImageCallback: (contactImage: ContactImage) -> Unit

    private lateinit var evaSendCompleteCallback: (peer: Peer, info: String, nonce: ULong) -> Unit
    private lateinit var evaReceiveProgressCallback: (peer: Peer, info: String, progress: TransferProgress) -> Unit
    private lateinit var evaReceiveCompleteCallback: (peer: Peer, info: String, id: String, data: ByteArray?) -> Unit
    private lateinit var evaErrorCallback: (peer: Peer, exception: TransferException) -> Unit

    var identityInfo: IdentityInfo? = null

    init {
        messageHandlers[MessageId.MESSAGE] = ::onMessagePacket
        messageHandlers[MessageId.ACK] = ::onAckPacket
        messageHandlers[MessageId.ATTACHMENT_REQUEST] = ::onAttachmentRequestPacket
        messageHandlers[MessageId.ATTACHMENT] = ::onAttachmentPacket
        messageHandlers[MessageId.CONTACT_IMAGE_REQUEST] = ::onContactImageRequestPacket
        messageHandlers[MessageId.CONTACT_IMAGE] = ::onContactImagePacket

        evaProtocolEnabled = true
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
                        val chatMessage = message.copy(identityInfo = identityInfo)
                        sendMessage(chatMessage)
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

        setOnEVASendCompleteCallback(::onEVASendCompleteCallback)
        setOnEVAReceiveProgressCallback(::onEVAReceiveProgressCallback)
        setOnEVAReceiveCompleteCallback(::onEVAReceiveCompleteCallback)
        setOnEVAErrorCallback(::onEVAErrorCallback)
    }

    /**
     * Set callback entry points
     */
    fun setOnMessageCallback(
        f: (instance: PeerChatCommunity, peer: Peer, chatMessage: ChatMessage) -> Unit
    ) {
        this.onMessageCallback = f
    }

    fun setOnContactImageRequestCallback(
        f: (instance: PeerChatCommunity, peer: Peer) -> Unit
    ) {
        this.onContactImageRequestCallback = f
    }

    fun setOnContactImageCallback(
        f: (contactImage: ContactImage) -> Unit
    ) {
        this.onContactImageCallback = f
    }

    fun setEVAOnSendCompleteCallback(f: (peer: Peer, info: String, nonce: ULong) -> Unit) {
        this.evaSendCompleteCallback = f
    }

    fun setEVAOnReceiveProgressCallback(f: (peer: Peer, info: String, progress: TransferProgress) -> Unit) {
        this.evaReceiveProgressCallback = f
    }

    fun setEVAOnReceiveCompleteCallback(f: (peer: Peer, info: String, id: String, data: ByteArray?) -> Unit) {
        this.evaReceiveCompleteCallback = f
    }

    fun setEVAOnErrorCallback(f: (peer: Peer, exception: TransferException) -> Unit) {
        this.evaErrorCallback = f
    }

    /**
     * Send actions
     */
    fun sendMessageWithTransaction(
        message: String,
        transaction_hash: ByteArray?,
        recipient: PublicKey,
        identityInfo: IdentityInfo? = null
    ) {
        val chatMessage = createOutgoingChatMessage(message, null, transaction_hash, recipient, identityInfo)
        database.addMessage(chatMessage)
        sendMessage(chatMessage)
    }

    fun sendMessage(message: String, recipient: PublicKey, identityInfo: IdentityInfo? = null) {
        val chatMessage = createOutgoingChatMessage(message, null, null, recipient, identityInfo)
        database.addMessage(chatMessage)

        sendMessage(chatMessage)
    }

    fun sendImage(file: File, recipient: PublicKey, identityInfo: IdentityInfo? = null) {
        val hash = file.name.hexToBytes()
        val attachment = MessageAttachment(MessageAttachment.TYPE_IMAGE, file.length(), hash)
        val chatMessage = createOutgoingChatMessage("", attachment, null, recipient, identityInfo)
        database.addMessage(chatMessage)

        sendMessage(chatMessage)
    }

    fun sendFile(
        file: File,
        fileName: String,
        recipient: PublicKey,
        identityInfo: IdentityInfo? = null
    ) {
        val hash = file.name.hexToBytes()
        val attachment = MessageAttachment(MessageAttachment.TYPE_FILE, file.length(), hash)
        val chatMessage = createOutgoingChatMessage(fileName, attachment, null, recipient, identityInfo)

        database.addMessage(chatMessage)
        sendMessage(chatMessage)
    }

    fun sendIdentityAttribute(
        attributeMessage: String,
        identityAttribute: IdentityAttribute,
        recipient: PublicKey,
        identityInfo: IdentityInfo? = null
    ) {
        val serialized = identityAttribute.serialize()
        val attachment = MessageAttachment(
            MessageAttachment.TYPE_IDENTITY_ATTRIBUTE,
            serialized.size.toLong(),
            serialized
        )
        val chatMessage = createOutgoingChatMessage(attributeMessage, attachment, null, recipient, identityInfo)

        database.addMessage(chatMessage)
        sendMessage(chatMessage)
    }

    fun sendLocation(
        location: Location,
        addressLine: String,
        recipient: PublicKey,
        identityInfo: IdentityInfo? = null
    ) {
        val json = JSONObject()
        json.put("latitude", location.latitude)
        json.put("longitude", location.longitude)
        json.put("address_line", addressLine)
        val serialized = json.toString().toByteArray()

        val attachment = MessageAttachment(MessageAttachment.TYPE_LOCATION, serialized.size.toLong(), serialized)
        val chatMessage = createOutgoingChatMessage(addressLine, attachment, null, recipient, identityInfo)

        database.addMessage(chatMessage)
        sendMessage(chatMessage)
    }

    fun sendTransferRequest(
        description: String?,
        transferRequest: TransferRequest,
        recipient: PublicKey,
        identityInfo: IdentityInfo? = null
    ) {
        val serialized = transferRequest.serialize()

        val attachment = MessageAttachment(MessageAttachment.TYPE_TRANSFER_REQUEST, serialized.size.toLong(), serialized)
        val chatMessage = createOutgoingChatMessage(description ?: "", attachment, null, recipient, identityInfo)

        database.addMessage(chatMessage)
        sendMessage(chatMessage)
    }

    fun sendContact(
        contact: Contact,
        recipient: PublicKey,
        identityInfo: IdentityInfo? = null
    ) {
        val serialized = contact.serialize()
        val contactMessage = "${contact.name} ${contact.publicKey.keyToBin().toHex()}"
        val attachment = MessageAttachment(MessageAttachment.TYPE_CONTACT, serialized.size.toLong(), serialized)
        val chatMessage = createOutgoingChatMessage(contactMessage, attachment, null, recipient, identityInfo)

        database.addMessage(chatMessage)
        sendMessage(chatMessage)
    }

    private fun sendMessage(chatMessage: ChatMessage) {
        val mid = chatMessage.recipient.keyToHash().toHex()
        val peer = getPeers().find { it.mid == mid }

        identityInfo = chatMessage.identityInfo

        if (peer != null) {
            val payload = MessagePayload(
                chatMessage.id,
                chatMessage.message,
                chatMessage.attachment?.type ?: "",
                chatMessage.attachment?.size ?: 0L,
                chatMessage.attachment?.content ?: ByteArray(0),
                chatMessage.transactionHash,
                chatMessage.identityInfo
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

    fun sendAck(peer: Peer, id: String) {
        val payload = AckPayload(id)
        val packet = serializePacket(MessageId.ACK, payload)
        logger.debug { "-> $payload" }
        send(peer, packet)
    }

    fun sendAttachmentRequest(peer: Peer, id: String) {
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

        if (evaProtocolEnabled) evaSendBinary(
            peer,
            EVAId.EVA_PEERCHAT_ATTACHMENT,
            id,
            packet
        ) else send(peer, packet)
    }

    fun sendContactImageRequest(recipient: PublicKey) {
        Log.d("VTLOG", "SEND CONTACT IMAGE REQUEST")
        val mid = recipient.keyToHash().toHex()
        val peer = getPeers().find { it.mid == mid }

        if (peer != null) {
            val payload = ContactImageRequestPayload(myPeer.publicKey)
            val packet = serializePacket(MessageId.CONTACT_IMAGE_REQUEST, payload)
            logger.debug { "-> $payload" }
            send(peer, packet)
        } else {
            Log.d("PeerChat", "Peer $mid not online")
        }
    }

    fun sendContactImage(peer: Peer, contactImage: ContactImage) {
        Log.d("VTLOG", "SEND CONTACT IMAGE")
        val payload = ContactImagePayload(contactImage.publicKey, contactImage.imageHash, contactImage.image)
        val packet = serializePacket(MessageId.CONTACT_IMAGE, payload, encrypt = true, recipient = peer)
        logger.debug { "-> $payload" }
        send(peer, packet)
    }

    /**
     * On packet receipt
     */
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

    private fun onContactImageRequestPacket(packet: Packet) {
        val (peer, _) = packet.getAuthPayload(ContactImageRequestPayload.Deserializer)

        logger.debug { "<- $peer" }
        onContactImageRequest(peer)
    }

    private fun onContactImagePacket(packet: Packet) {
        Log.d("VTLOG", "CONTACT IMAGE PACKET RECEIVED")

        val (peer, payload) = packet.getDecryptedAuthPayload(
            ContactImagePayload.Deserializer, myPeer.key as PrivateKey
        )

        Log.d("VTLOG", "CONTACT IMAGE CONTENTS PEER: $peer")
        Log.d("VTLOG", "CONTACT IMAGE CONTENTS: ${payload.imageHash}")

        logger.debug { "<- $payload" }
        onContactImage(payload)
    }

    /**
     * On receipt handlers
     */
    private fun onMessage(peer: Peer, payload: MessagePayload) {
        Log.d("PeerChat", "onMessage from $peer: $payload")

        val chatMessage = createIncomingChatMessage(peer, payload)

        if (this::onMessageCallback.isInitialized) {
            this.onMessageCallback(this@PeerChatCommunity, peer, chatMessage)

            return
        }

        try {
            database.addMessage(chatMessage)
        } catch (e: SQLiteConstraintException) {
            e.printStackTrace()
        }

        Log.d("PeerChat", "Sending ack to ${chatMessage.id}")
        sendAck(peer, chatMessage.id)

        // Request attachment
        if (chatMessage.attachment != null) {
            when (chatMessage.attachment.type) {
                MessageAttachment.TYPE_IDENTITY_ATTRIBUTE -> return
                MessageAttachment.TYPE_CONTACT -> return
                MessageAttachment.TYPE_LOCATION -> return
                MessageAttachment.TYPE_TRANSFER_REQUEST -> return
                else -> sendAttachmentRequest(peer, chatMessage.attachment.content.toHex())
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

    private fun onContactImageRequest(peer: Peer) {
        if (this::onContactImageRequestCallback.isInitialized) {
            this.onContactImageRequestCallback(this@PeerChatCommunity, peer)

            return
        }
    }

    private fun onContactImage(payload: ContactImagePayload) {
        Log.d("VTLOG", "ON CONTACT IMAGE with Payload image hash: ${payload.imageHash}")
        if (this::onContactImageCallback.isInitialized) {
            this.onContactImageCallback(ContactImage(payload.publicKey, payload.imageHash, payload.image))

            return
        }
    }

    /**
     * Creation of objects from payload
     */
    private fun createOutgoingChatMessage(
        message: String,
        attachment: MessageAttachment?,
        transaction_hash: ByteArray?,
        recipient: PublicKey,
        identityInfo: IdentityInfo? = null
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
            transactionHash = transaction_hash,
            identityInfo = identityInfo
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
            transactionHash = message.transactionHash,
            identityInfo = message.identityInfo
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

    /**
     * EVA Callbacks
     */
    private fun onEVASendCompleteCallback(peer: Peer, info: String, nonce: ULong) {
        Log.d("PeerChat", "ON EVA send complete callback for '$info'")

        if (info != EVAId.EVA_PEERCHAT_ATTACHMENT) return

        if (this::evaSendCompleteCallback.isInitialized) {
            this.evaSendCompleteCallback(peer, info, nonce)
        }
    }

    private fun onEVAReceiveProgressCallback(peer: Peer, info: String, progress: TransferProgress) {
        Log.d("PeerChat", "ON EVA receive progress callback for '$info'")

        if (info != EVAId.EVA_PEERCHAT_ATTACHMENT) return

        if (this::evaReceiveProgressCallback.isInitialized) {
            this.evaReceiveProgressCallback(peer, info, progress)
        }
    }

    private fun onEVAReceiveCompleteCallback(peer: Peer, info: String, id: String, data: ByteArray?) {
        Log.d("PeerChat", "ON EVA receive complete callback for '$info'")

        if (info != EVAId.EVA_PEERCHAT_ATTACHMENT) return

        data?.let {
            val packet = Packet(peer.address, it)
            onAttachmentPacket(packet)
        }

        if (this::evaReceiveCompleteCallback.isInitialized) {
            this.evaReceiveCompleteCallback(peer, info, id, data)
        }
    }

    private fun onEVAErrorCallback(peer: Peer, exception: TransferException) {
        Log.d("PeerChat", "ON EVA error callback for '${exception.info}'")

        if (exception.info != EVAId.EVA_PEERCHAT_ATTACHMENT) return

        if (this::evaErrorCallback.isInitialized) {
            this.evaErrorCallback(peer, exception)
        }
    }

    /**
     * Database functions
     */
    fun getDatabase(): PeerChatStore {
        return database
    }

    fun createContactStateTable() {
        database.createContactStateTable()
    }

    fun createContactImageTable() {
        database.createContactImageTable()
    }

    object MessageId {
        const val MESSAGE = 1
        const val ACK = 2
        const val ATTACHMENT_REQUEST = 3
        const val ATTACHMENT = 4
        const val CONTACT_IMAGE_REQUEST = 5
        const val CONTACT_IMAGE = 6
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
