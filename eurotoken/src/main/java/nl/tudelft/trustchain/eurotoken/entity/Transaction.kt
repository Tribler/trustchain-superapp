package nl.tudelft.trustchain.eurotoken.entity

import nl.tudelft.ipv8.keyvault.PublicKey
import java.util.*

data class Transaction(
    /**
     * The unique transaction ID.
     */
    val id: String,

    /**
     * The transaction amount in cent.
     */
    val amount: Int,

    /**
     * The public key of the transaction sender.
     */
    val sender: PublicKey,

    /**
     * The public key of the transaction recipient.
     */
    val recipient: PublicKey,

    /**
     * True if we are the sender, false otherwise.
     */
    val outgoing: Boolean,

    /**
     * The timestamp when the transaction was sent/received.
     */
    val timestamp: Date,

    /**
     * True if the transaction has been confirmed.
     */
    val confirmed: Boolean,

    /**
     * True if the transaction has been delivered to the network.
     */
    val sent: Boolean,

    /**
     * True if the transaction has been received.
     */
    val received: Boolean,

    /**
     * True if the transaction has been seen.
     */
    val read: Boolean

    )
