package nl.tudelft.trustchain.valuetransfer.passport.entity

import android.graphics.Bitmap

data class PersonDetails(
    var name: String? = null,
    var surname: String? = null,
    var personalNumber: String? = null,
    var gender: String? = null,
    var dateOfBirth: Long? = null,
    var dateOfExpiry: Long? = null,
    var serialNumber: String? = null,
    var nationality: String? = null,
    var issuerAuthority: String? = null,
    var faceImage: Bitmap? = null,
) {
    override fun toString(): String {
        return "NAME: $name, " +
            "SURNAME: $surname, " +
            "PERSONAL_NUMBER: $personalNumber, " +
            "GENDER: $gender, " +
            "DATEOFBIRTH: $dateOfBirth, " +
            "DATEOFEXPIRY: $dateOfExpiry. " +
            "DOCUMENTNUMBER: $serialNumber, " +
            "NATIONALITY: $nationality, " +
            "ISSUER: $issuerAuthority, " +
            "FACEIMAGE: ${faceImage != null}"
    }
}
