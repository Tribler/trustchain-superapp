package nl.tudelft.trustchain.detoks

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable

class Token(val unique_id: String, val public_key: ByteArray) : Serializable {

    override fun serialize(): ByteArray {
        val idBytes = unique_id.toByteArray(Charsets.UTF_8)
        val buffer = ByteArray(idBytes.size + public_key.size)
        System.arraycopy(idBytes, 0, buffer, 0, idBytes.size)
        System.arraycopy(public_key, 0, buffer, idBytes.size, public_key.size)
        return buffer
    }

    companion object Deserializer : Deserializable<Token> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<Token, Int> {
            val idLength = buffer.size - 32 // 32 is the length of a peer public key
            val id = String(buffer.copyOfRange(offset, offset + idLength), Charsets.UTF_8)
            val publicKey = buffer.copyOfRange(offset + idLength, offset + buffer.size)
            return Pair(Token(id, publicKey), buffer.size)
        }
    }
}

