package nl.tudelft.trustchain.foc.community

import android.util.Log
import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.deserializeVarLen
import nl.tudelft.ipv8.messaging.serializeVarLen
import org.apache.commons.lang3.SerializationUtils
import java.io.Serializable
import java.util.UUID

data class FOCPullRequestMessage(val ids: HashSet<UUID>) :
    Serializable,
    nl.tudelft.ipv8.messaging.Serializable {
    override fun serialize(): ByteArray {
        return serializeVarLen(SerializationUtils.serialize(ids))
    }

    companion object Deserializer : Deserializable<FOCPullRequestMessage> {
        override fun deserialize(
            buffer: ByteArray,
            offset: Int
        ): Pair<FOCPullRequestMessage, Int> {
            val (payload, localOffset) = deserializeVarLen(buffer, offset)
            val set = FOCPullRequestMessage(SerializationUtils.deserialize(payload))

            Log.i("pull-based", "PullRequestMessage: ${localOffset - offset} Bytes")
            return Pair(set, offset)
        }
    }
}
