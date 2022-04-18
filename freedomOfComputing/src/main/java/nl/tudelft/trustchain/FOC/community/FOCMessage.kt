package nl.tudelft.trustchain.FOC.community

import nl.tudelft.ipv8.messaging.Deserializable

data class FOCMessage(val message: String) : nl.tudelft.ipv8.messaging.Serializable {
    override fun serialize(): ByteArray {
        return message.toByteArray()
    }

    companion object Deserializer : Deserializable<FOCMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<FOCMessage, Int> {
            var toReturn = buffer.toString(Charsets.UTF_8)
            return Pair(FOCMessage(toReturn), buffer.size)
        }
    }
}
