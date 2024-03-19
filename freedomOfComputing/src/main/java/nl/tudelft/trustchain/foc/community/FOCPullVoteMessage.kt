package nl.tudelft.trustchain.foc.community

import nl.tudelft.ipv8.messaging.Deserializable
import java.io.Serializable
import nl.tudelft.ipv8.messaging.deserializeVarLen
import org.apache.commons.lang3.SerializationUtils
import nl.tudelft.ipv8.messaging.serializeVarLen

data class FOCPullVoteMessage(val voteMap: HashMap<String, HashSet<FOCVote>>) :
    Serializable,
    nl.tudelft.ipv8.messaging.Serializable {
    override fun serialize(): ByteArray {
        return serializeVarLen(SerializationUtils.serialize(voteMap))
    }

    companion object Deserializer : Deserializable<FOCPullVoteMessage> {
        override fun deserialize(
            buffer: ByteArray,
            offset: Int
        ): Pair<FOCPullVoteMessage, Int> {
            val (payload, _) = deserializeVarLen(buffer, offset)
            val set = FOCPullVoteMessage(SerializationUtils.deserialize(payload))

            return Pair(set, offset)
        }
    }
}
