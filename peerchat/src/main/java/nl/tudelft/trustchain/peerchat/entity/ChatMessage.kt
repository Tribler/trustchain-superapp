package nl.tudelft.trustchain.peerchat.entity

import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.trustchain.peerchat.ui.conversation.MessageAttachment
import java.util.*

data class ChatMessage(
    /**
     * The unique message ID.
     */
    val id: String,

    /**
     * The message content.
     */
    val message: String,

    /**
     * An optional message attachment.
     */
    val attachment: MessageAttachment?,

    /**
     * The public key of the message sender.
     */
    val sender: PublicKey,

    /**
     * The public key of the message recipient.
     */
    val recipient: PublicKey,

    /**
     * True if we are the sender, false otherwise.
     */
    val outgoing: Boolean,

    /**
     * The timestamp when the message was sent/received.
     */
    val timestamp: Date,

    /**
     * True if the message has been acknowledged yet.
     */
    val ack: Boolean,

    /**
     * True if the message has been read (for incoming messages).
     */
    val read: Boolean,

    /**
     * True if the attachment has been fetched and stored locally (for incoming messages).
     */
    val attachmentFetched: Boolean,

    /**
     * Optional reference to a TrustChain proposal block that includes a TrustChain transaction
     */
    val transactionHash: ByteArray?

)
