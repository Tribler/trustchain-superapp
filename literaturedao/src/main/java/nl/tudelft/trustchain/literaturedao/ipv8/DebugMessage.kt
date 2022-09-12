package nl.tudelft.trustchain.literaturedao.ipv8

import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.Deserializable


data class DebugMessage(val message: String) : Serializable {
    override fun serialize(): ByteArray {
        return message.toByteArray()
    }

    companion object Deserializer : Deserializable<DebugMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<DebugMessage, Int> {
            return Pair(DebugMessage(buffer.toString(Charsets.UTF_8)), buffer.size)
        }
    }
}
