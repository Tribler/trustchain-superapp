package nl.tudelft.trustchain.eurotoken.community

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import mu.KotlinLogging
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.trustchain.eurotoken.community.MessagePayload
import java.io.FileOutputStream
import java.util.*

private val logger = KotlinLogging.logger {}

class PeerChatCommunity(
    private val context: Context
) : Community() {
    override val serviceId = "ac9c01202e8d01e5f7d3cec88085dd842267c273"

    init {
        //messageHandlers[MessageId.MESSAGE] = ::onMessagePacket
    }

    override fun load() {
        super.load()
    }

    private fun connectToGateway(payment_id: String, public_key: String , ip: String, port: Int) {
        //Peer(public_key)
        //make peer
        //val mid = chatMessage.recipient.keyToHash().toHex()
        //val peer = getPeers().find { it.mid == mid }

        val payload = MessagePayload( payment_id )

        val packet = serializePacket(
            MessageId.MESSAGE,
            payload
        )

        send(peer, packet)
    }

    private fun onMessagePacket(packet: Packet) {
        val (peer, payload) = packet.getDecryptedAuthPayload(
            MessagePayload.Deserializer, myPeer.key as PrivateKey)
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
            AttachmentPayload.Deserializer, myPeer.key as PrivateKey)
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

        // Request attachment
        if (chatMessage.attachment != null) {
            sendAttachmentRequest(peer, chatMessage.attachment.content.toHex())
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
