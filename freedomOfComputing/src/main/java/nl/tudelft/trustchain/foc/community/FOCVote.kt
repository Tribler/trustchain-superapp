package nl.tudelft.trustchain.foc.community

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.deserializeVarLen
import nl.tudelft.ipv8.messaging.serializeVarLen
import java.io.Serializable

enum class VoteType {
    UP,
    DOWN
}

data class FOCVote(val memberId: String, val voteType: VoteType) :
    Serializable,
    nl.tudelft.ipv8.messaging.Serializable {
    override fun serialize(): ByteArray {
        return serializeVarLen(memberId.toByteArray(Charsets.UTF_8)) +
            serializeVarLen(voteType.toString().toByteArray(Charsets.UTF_8))
    }

    companion object Deserializer : Deserializable<FOCVote> {
        override fun deserialize(
            buffer: ByteArray,
            offset: Int
        ): Pair<FOCVote, Int> {
            var localOffset = offset
            val (mid, midSize) = deserializeVarLen(buffer, localOffset)
            localOffset += midSize
            val (voteType, voteTypeSize) = deserializeVarLen(buffer, localOffset)
            localOffset += voteTypeSize
            val payload =
                FOCVote(
                    mid.toString(Charsets.UTF_8),
                    VoteType.valueOf(voteType.toString(Charsets.UTF_8))
                )
            return Pair(payload, localOffset - offset)
        }
    }
}
