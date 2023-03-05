import nl.tudelft.ipv8.messaging.Deserializable

class MyMessage(val message: String) : nl.tudelft.ipv8.messaging.Serializable {
    override fun serialize(): ByteArray {
        return message.toByteArray()
    }

    companion object Deserializer : Deserializable<MyMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<MyMessage, Int> {
            return Pair(MyMessage(buffer.toString(Charsets.UTF_8)), buffer.size)
        }
    }
}
