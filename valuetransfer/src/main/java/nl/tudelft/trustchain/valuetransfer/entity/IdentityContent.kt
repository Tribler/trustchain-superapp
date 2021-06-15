package nl.tudelft.trustchain.valuetransfer.entity

import java.util.*

interface IdentityContent{}

data class PersonalIdentity(
    /**
     * Given Names  of the identity or name of the business.
     */
    val givenNames: String,

    /**
     * Surname  of the identity.
     */
    val surname: String,

    /**
     * Gender of the identity.
     */
    val gender: String,

    /**
     * Date of birth of the identity.
     */
    val dateOfBirth: Date,

    /**
     * Place of birth of the identity.
     */
    val placeOfBirth: String,

    /**
     * Nationality of the identity.
     */
    val nationality: String,

    /**
     * Personal number of the identity.
     */
    val personalNumber: Long,

    /**
     * Document number of the identity.
     */
    val documentNumber: String,
)  : IdentityContent {

    override fun toString() : String {
        return "$givenNames $surname ($gender) born on $dateOfBirth at $placeOfBirth as $nationality. Personal number $personalNumber, document number $documentNumber"
    }
}

data class BusinessIdentity(

    /**
     * Given Names  of the identity or name of the business.
     */
    val companyName: String,

    /**
     * Established since.
     */
    val dateOfBirth: Date,

    /**
     * Surname  of the identity.
     */
    val residence: String,

    /**
     * Gender of the identity.
     */
    val areaOfExpertise: String,
)  : IdentityContent {

    override fun toString() : String {
        return "$companyName ($residence) established in $dateOfBirth, specialised in $areaOfExpertise"
    }
}
