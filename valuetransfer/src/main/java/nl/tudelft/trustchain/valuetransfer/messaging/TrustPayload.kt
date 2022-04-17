package nl.tudelft.trustchain.valuetransfer.messaging

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.deserializeVarLen
import nl.tudelft.ipv8.messaging.serializeVarLen

class TrustPayload(val scores: ByteArray) : Serializable {
    override fun serialize(): ByteArray {
        return serializeVarLen(scores)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TrustPayload

        if (!scores.contentEquals(other.scores)) return false
        return true
    }

    companion object Deserializer : Deserializable<TrustPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<TrustPayload, Int> {
            var localOffset = 0
            val (scoresKey, scoresKeySize) = deserializeVarLen(buffer, offset + localOffset)
            localOffset += scoresKeySize
            val payload = TrustPayload(
                scoresKey
            )
            return Pair(payload, localOffset)
        }
    }
}
