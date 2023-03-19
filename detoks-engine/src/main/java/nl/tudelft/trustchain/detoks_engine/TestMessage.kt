import nl.tudelft.ipv8.messaging.Deserializable

class TestMessage(val message: String) : nl.tudelft.ipv8.messaging.Serializable {
    override fun serialize(): ByteArray {
        return message.toByteArray()
    }

    companion object Deserializer : Deserializable<TestMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<TestMessage, Int> {
            return Pair(TestMessage(buffer.toString(Charsets.UTF_8)), buffer.size)
        }
    }
}
