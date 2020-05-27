package nl.tudelft.trustchain.peerchat.community

import nl.tudelft.ipv8.messaging.*

data class MessagePayload(
    val id: String,
    val message: String
) : Serializable {
    override fun serialize(): ByteArray {
        return serializeVarLen(id.toByteArray()) +
            serializeVarLen(message.toByteArray())
    }

    companion object Deserializer : Deserializable<MessagePayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<MessagePayload, Int> {
            var localOffset = offset
            val (id, idSize) = deserializeVarLen(buffer, localOffset)
            localOffset += idSize
            val (message, messageSize) = deserializeVarLen(buffer, localOffset)
            localOffset += messageSize
            return Pair(
                MessagePayload(id.toString(Charsets.UTF_8), message.toString(Charsets.UTF_8)),
                localOffset - offset
            )
        }
    }
}
