package nl.tudelft.trustchain.valuetransfer.entity

import nl.tudelft.ipv8.keyvault.PublicKey // OK3
import java.util.*

data class Identity(
    /**
     * The unique identity ID.
     */
    val id: String,

    /**
     * The public key of the identity.
     */
    val publicKey: PublicKey,

    /**
     * Content of the identity, depending on type.
     */
    val content: PersonalIdentity,

    /**
     * Identity added on date.
     */
    val added: Date,

    /**
     * Identity modified on date.
     */
    var modified: Date,
)

data class PersonalIdentity(
    /**
     * Given Names  of the identity or name of the business.
     */
    var givenNames: String,

    /**
     * Surname  of the identity.
     */
    var surname: String,

    /**
     * Gender of the identity.
     */
    var gender: String,

    /**
     * Date of birth of the identity.
     */
    var dateOfBirth: Date,

    /**
     * Place of birth of the identity.
     */
    var placeOfBirth: String,

    /**
     * Nationality of the identity.
     */
    var nationality: String,

    /**
     * Personal number of the identity.
     */
    var personalNumber: Long,

    /**
     * Document number of the identity.
     */
    var documentNumber: String,

    /**
     * Identity is verified when NFC is also used during importing
     */
    var verified: Boolean,

    /**
     * Identity expiration date
     */
    var dateOfExpiry: Date,
) {
    override fun toString(): String {
        return "$givenNames $surname ($gender) born on $dateOfBirth as $nationality. Personal number $personalNumber, document number $documentNumber. Identity is verified: $verified. Expires on $dateOfExpiry"
    }
}
