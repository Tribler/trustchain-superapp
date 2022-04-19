package nl.tudelft.trustchain.eurotoken.community

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.deserializeVarLen
import nl.tudelft.ipv8.messaging.serializeVarLen

/**
 * This class is used to serialize and deserialize
 * the transactions payload message. In essence, this payload
 * encodes public keys such that trust scores can be updated.
 * Used by EuroTokenCommunity
 */
class TransactionsPayload (    val id: String,
                               val data: ByteArray
) : Serializable {
    override fun serialize(): ByteArray {
        return serializeVarLen(id.toByteArray()) + serializeVarLen(data)
    }

    companion object Deserializer : Deserializable<TransactionsPayload> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<TransactionsPayload, Int> {
            var localOffset = offset
            val (id, idSize) = deserializeVarLen(buffer, localOffset)
            localOffset += idSize
            val (data, dataSize) = deserializeVarLen(buffer, localOffset)
            localOffset += dataSize
            return Pair(
                TransactionsPayload(
                    id.toString(Charsets.UTF_8),
                    data
                ),
                localOffset - offset
            )
        }
    }
}
