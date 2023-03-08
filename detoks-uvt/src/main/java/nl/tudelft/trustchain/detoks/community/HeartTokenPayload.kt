package nl.tudelft.trustchain.detoks.community

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.deserializeVarLen
import nl.tudelft.ipv8.messaging.serializeVarLen

class HeartTokenPayload constructor(val id: String, val token: String) : Serializable {
    override fun serialize(): ByteArray {
        return serializeVarLen(id.toByteArray()) + serializeVarLen(token.toByteArray())
    }

    companion object Deserializer : Deserializable<HeartTokenPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<HeartTokenPayload, Int> {
            var localOffset = offset
            val (id, idSize) = deserializeVarLen(buffer, localOffset)
            localOffset += idSize
            val (token, tokenSize) = deserializeVarLen(buffer, localOffset)
            localOffset += tokenSize

            return Pair(
                HeartTokenPayload(id.toString(Charsets.UTF_8), token.toString(Charsets.UTF_8)),
                localOffset - offset
            )
        }
    }
}

