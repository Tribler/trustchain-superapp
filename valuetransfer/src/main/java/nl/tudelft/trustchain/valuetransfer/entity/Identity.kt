package nl.tudelft.trustchain.valuetransfer.entity

import nl.tudelft.ipv8.keyvault.PublicKey
import java.util.*

data class Identity(
    /**
     * The unique message ID.
     */
    val id: String,

    /**
     * The public key of the identity.
     */
    val publicKey: PublicKey,

    /**
     * Type of the identity: 0 for personal, 1 for business identity.
     */
    val type: String,

    /**
     * Content of the identity, depending on type.
     */
    val content: IdentityContent?,

    /**
     * Identity added on date.
     */
    val added: Date,

    /**
     * Identity modified on date.
     */
    val modified: Date,
)
