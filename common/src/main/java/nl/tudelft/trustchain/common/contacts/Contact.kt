package nl.tudelft.trustchain.common.contacts

import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import org.json.JSONObject
import java.io.Serializable

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

    fun serialize(): ByteArray {
        val json = JSONObject()
        json.put(NAME, name)
        json.put(PUBLIC_KEY, publicKey.keyToBin().toHex())

        return json.toString().toByteArray()
    }

    companion object : Deserializable<Contact> {
        const val PUBLIC_KEY = "public_key"
        const val NAME = "name"

        override fun deserialize(
            buffer: ByteArray,
            offset: Int
        ): Pair<Contact, Int> {
            val offsetBuffer = buffer.copyOfRange(0, buffer.size)
            val json = JSONObject(offsetBuffer.decodeToString())
            val name = json.getString(NAME)
            val publicKeyString = json.getString(PUBLIC_KEY)
            val publicKey = defaultCryptoProvider.keyFromPublicBin(publicKeyString.hexToBytes())

            return Pair(Contact(name, publicKey), 0)
        }
    }
}
