package nl.tudelft.trustchain.detoks

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable

class Token(val unique_id: String) : Serializable {

    override fun serialize(): ByteArray {
        return unique_id.toByteArray()
    }

    companion object Deserializer : Deserializable<Token> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<Token, Int> {
            return Pair(Token(buffer.toString(Charsets.UTF_8)), buffer.size)
        }
    }
}
