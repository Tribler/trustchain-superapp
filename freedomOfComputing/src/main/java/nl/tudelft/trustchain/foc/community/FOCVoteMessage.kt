package nl.tudelft.trustchain.foc.community

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.SERIALIZED_USHORT_SIZE
import nl.tudelft.ipv8.messaging.deserializeUShort
import nl.tudelft.ipv8.messaging.deserializeVarLen
import nl.tudelft.ipv8.messaging.serializeUShort
import nl.tudelft.ipv8.messaging.serializeVarLen
import java.io.Serializable

data class FOCVoteMessage(val fileName: String, val focVote: FOCVote, val TTL: Int) :
    Serializable,
    nl.tudelft.ipv8.messaging.Serializable {
    override fun serialize(): ByteArray {
        return serializeVarLen(fileName.toByteArray(Charsets.UTF_8)) +
            serializeVarLen(focVote.memberId.toByteArray(Charsets.UTF_8)) +
            serializeVarLen(focVote.voteType.toString().toByteArray(Charsets.UTF_8)) +
            serializeUShort(TTL)
    }

    companion object Deserializer : Deserializable<FOCVoteMessage> {
        override fun deserialize(
            buffer: ByteArray,
            offset: Int
        ): Pair<FOCVoteMessage, Int> {
            var localOffset = offset
            val (fileName, fileNameSize) = deserializeVarLen(buffer, localOffset)
            localOffset += fileNameSize
            val (mid, midSize) = deserializeVarLen(buffer, localOffset)
            localOffset += midSize
            val (voteType, voteTypeSize) = deserializeVarLen(buffer, localOffset)
            localOffset += voteTypeSize
            val ttl = deserializeUShort(buffer, localOffset)
            localOffset += SERIALIZED_USHORT_SIZE
            val payload =
                FOCVoteMessage(
                    fileName.toString(Charsets.UTF_8),
                    FOCVote(
                        mid.toString(Charsets.UTF_8),
                        VoteType.valueOf(voteType.toString(Charsets.UTF_8))
                    ),
                    ttl
                )
            return Pair(payload, localOffset - offset)
        }
    }
}
