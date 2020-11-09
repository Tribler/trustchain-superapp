package nl.tudelft.trustchain.eurotoken.community

import nl.tudelft.ipv8.messaging.*

class MessagePayload constructor( val id: String ) : Serializable {
    override fun serialize(): ByteArray {
        return serializeVarLen(id.toByteArray())
    }

    companion object Deserializer : Deserializable<MessagePayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<MessagePayload, Int> {
            var localOffset = offset
            val (id, idSize) = deserializeVarLen(buffer, localOffset)
            localOffset += idSize

            return Pair(
                MessagePayload( id.toString(Charsets.UTF_8) ),
                localOffset - offset
            )
        }
    }
}
