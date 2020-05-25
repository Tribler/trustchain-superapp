package nl.tudelft.trustchain.peerchat.entity

import nl.tudelft.ipv8.keyvault.PublicKey
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
     * True if the message has been read.
     */
    val read: Boolean
)
