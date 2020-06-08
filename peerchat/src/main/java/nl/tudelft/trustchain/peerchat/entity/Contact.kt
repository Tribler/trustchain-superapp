package nl.tudelft.trustchain.peerchat.entity

import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.util.toHex

data class Contact(
    val name: String,
    val publicKey: PublicKey
) {
    val mid = publicKey.keyToHash().toHex()

    override fun equals(other: Any?): Boolean {
        return other is Contact &&
            publicKey.keyToBin().contentEquals(other.publicKey.keyToBin()) &&
            name == other.name
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + publicKey.hashCode()
        return result
    }
}
