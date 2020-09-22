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
    val sender: String,

    /**
     * The public key of the transaction recipient.
     */
    val recipient: String,

    /**
     * True if we are the sender, false otherwise.
     */
    val outgoing: Boolean,

    /**
     * The timestamp when the transaction was sent/received.
     */
    val timestamp: Date,

    /**
     * True if the transaction has been confirmed yet.
     */
    val confirmed: Boolean
)
