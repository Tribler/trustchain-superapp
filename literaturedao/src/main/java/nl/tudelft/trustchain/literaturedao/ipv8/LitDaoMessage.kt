package nl.tudelft.trustchain.literaturedao.ipv8

import nl.tudelft.ipv8.messaging.Deserializable

data class LitDaoMessage(val message: String) : nl.tudelft.ipv8.messaging.Serializable {
    override fun serialize(): ByteArray {
        return message.toByteArray()
    }

    companion object Deserializer : Deserializable<LitDaoMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<LitDaoMessage, Int> {
            var toReturn = buffer.toString(Charsets.UTF_8)
            return Pair(LitDaoMessage(toReturn), buffer.size)
        }
    }
}
