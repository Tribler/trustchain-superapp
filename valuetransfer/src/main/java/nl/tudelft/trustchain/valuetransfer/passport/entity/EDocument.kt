package nl.tudelft.trustchain.valuetransfer.passport.entity

import java.security.PublicKey

data class EDocument(
    var documentType: String? = null,
    var personDetails: PersonDetails? = null,
    var documentPublicKey: PublicKey? = null
) {
    override fun toString(): String {
        return "TYPE: $documentType,/nPUBLIC_KEY: ${documentPublicKey.toString()},/nPERSON_DETAILS: ${personDetails.toString()}"
    }
}
