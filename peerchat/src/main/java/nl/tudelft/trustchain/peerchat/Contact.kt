package nl.tudelft.trustchain.peerchat

import nl.tudelft.ipv8.keyvault.PublicKey

data class Contact(
    val name: String,
    val publicKey: PublicKey
) {
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
