package nl.tudelft.trustchain.valuetransfer.entity

import java.util.*

interface IdentityContent{}

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
)  : IdentityContent {

    override fun toString() : String {
        return "$givenNames $surname ($gender) born on $dateOfBirth at $placeOfBirth as $nationality. Personal number $personalNumber, document number $documentNumber"
    }
}

data class BusinessIdentity(

    /**
     * Given Names  of the identity or name of the business.
     */
    var companyName: String,

    /**
     * Established since.
     */
    var dateOfBirth: Date,

    /**
     * Surname  of the identity.
     */
    var residence: String,

    /**
     * Gender of the identity.
     */
    var areaOfExpertise: String,
)  : IdentityContent {

    override fun toString() : String {
        return "$companyName ($residence) established in $dateOfBirth, specialised in $areaOfExpertise"
    }
}
