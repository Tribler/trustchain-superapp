package nl.tudelft.trustchain.peerchat.community

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.peerchat.db.PeerChatStore
import nl.tudelft.trustchain.peerchat.entity.ChatMessage
import java.sql.SQLException
import java.util.*

private val logger = KotlinLogging.logger {}

class PeerChatCommunity(
    private val database: PeerChatStore
) : Community() {
    override val serviceId = "ac9c01202e8d01e5f7d3cec88085dd842267c273"

    init {
        messageHandlers[MessageId.MESSAGE] = ::onMessagePacket
        messageHandlers[MessageId.ACK] = ::onAckPacket
    }

    override fun load() {
        super.load()

        scope.launch {
            while (isActive) {
                val messages = database.getUnacknowledgedMessages()
                Log.d("PeerChat", "Found ${messages.size} outgoing unack messages")
                messages.forEach { message ->
                    sendMessage(message)
                }
                delay(10000L)
            }
        }
    }

    fun sendMessage(message: String, recipient: PublicKey) {
        val chatMessage = createOutgoingChatMessage(message, recipient)
        database.addMessage(chatMessage)

        sendMessage(chatMessage)
    }

    private fun sendMessage(chatMessage: ChatMessage) {
        val payload = MessagePayload(chatMessage.id, chatMessage.message)
        // TODO: encrypt
        val packet = serializePacket(MessageId.MESSAGE, payload, true)

        val mid = chatMessage.recipient.keyToHash().toHex()
        val peer = getPeers().find { it.mid == mid }
        if (peer != null) {
            logger.debug { "-> $payload" }
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

    private fun onMessagePacket(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(MessagePayload.Deserializer)
        logger.debug { "<- $payload" }
        onMessage(peer, payload)
    }

    private fun onAckPacket(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(AckPayload.Deserializer)
        logger.debug { "<- $payload" }
        onAck(peer, payload)
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
    }

    private fun onAck(peer: Peer, payload: AckPayload) {
        Log.d("PeerChat", "onAck from $peer: $payload")

        try {
            database.ackMessage(payload.id)
        } catch (e: SQLiteConstraintException) {
            e.printStackTrace()
        }
    }

    private fun createOutgoingChatMessage(message: String, recipient: PublicKey): ChatMessage {
        val id = UUID.randomUUID().toString()
        return ChatMessage(
            id,
            message,
            myPeer.publicKey,
            recipient,
            true,
            Date(),
            ack = false,
            read = true
        )
    }

    private fun createIncomingChatMessage(peer: Peer, message: MessagePayload): ChatMessage {
        return ChatMessage(
            message.id,
            message.message,
            peer.publicKey,
            myPeer.publicKey,
            false,
            Date(),
            ack = false,
            read = false
        )
    }

    object MessageId {
        const val MESSAGE = 1
        const val ACK = 2
    }

    class Factory(
        private val database: PeerChatStore
    ) : Overlay.Factory<PeerChatCommunity>(PeerChatCommunity::class.java) {
        override fun create(): PeerChatCommunity {
            return PeerChatCommunity(database)
        }
    }
}
