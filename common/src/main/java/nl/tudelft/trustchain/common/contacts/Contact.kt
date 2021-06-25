package nl.tudelft.trustchain.common.contacts

import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import org.json.JSONObject

data class Contact(
    val name: String,
    val publicKey: PublicKey
) : Serializable {
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

    override fun serialize(): ByteArray {
        val json = JSONObject()
        json.put(ARG_NAME, name)
        json.put(ARG_PUBLIC_KEY, publicKey.keyToBin().toHex())
        return json.toString().toByteArray()
    }

    companion object : Deserializable<Contact> {
        val ARG_PUBLIC_KEY = "public_key"
        val ARG_NAME = "name"

        override fun deserialize(buffer: ByteArray, offset: Int): Pair<Contact, Int> {
            val offsetBuffer = buffer.copyOfRange(offset, buffer.size)
            val json = JSONObject(offsetBuffer.decodeToString())
            val jname = json.getString(ARG_NAME)
            val publicKeyBin = json.getString(ARG_PUBLIC_KEY).hexToBytes()
            val jpublicKey = defaultCryptoProvider.keyFromPublicBin(publicKeyBin)
            val contact = Contact(jname, jpublicKey)
            return Pair(contact, 0)
        }
    }
}
