package nl.tudelft.trustchain.foc.community

import nl.tudelft.ipv8.messaging.Deserializable

data class FOCVote(val memberId: String) : nl.tudelft.ipv8.messaging.Serializable {
    override fun serialize(): ByteArray {
        return memberId.toByteArray()
    }

    companion object Deserializer : Deserializable<FOCMessage> {
        override fun deserialize(
            buffer: ByteArray,
            offset: Int
        ): Pair<FOCMessage, Int> {
            val toReturn = buffer.toString(Charsets.UTF_8)
            return Pair(FOCMessage(toReturn), buffer.size)
        }
    }
}
