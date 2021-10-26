package nl.tudelft.trustchain.peerchat.entity

import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.trustchain.common.valuetransfer.entity.IdentityInfo

data class ContactState(
    /**
     * The public key of the contact.
     */
    val publicKey: PublicKey,

    /**
     * Archived status of contact
     */
    val isArchived: Boolean,

    /**
     * Mute status of contact
     */
    val isMuted: Boolean,

    /**
     * Blocked status of contact
     */
    val isBlocked: Boolean,

    /**
     * Identity info of contact
     */
    val identityInfo: IdentityInfo? = null
)
