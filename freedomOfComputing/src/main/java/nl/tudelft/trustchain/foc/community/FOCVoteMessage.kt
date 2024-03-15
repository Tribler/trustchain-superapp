package nl.tudelft.trustchain.foc.community

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.deserializeVarLen
import nl.tudelft.ipv8.messaging.serializeVarLen
import org.apache.commons.lang3.SerializationUtils
import java.io.Serializable

data class FOCVoteMessage(val fileName: String, val focSignedVote: FOCSignedVote) :
    Serializable,
    nl.tudelft.ipv8.messaging.Serializable {
    override fun serialize(): ByteArray {
        return serializeVarLen(fileName.toByteArray(Charsets.UTF_8)) +
            serializeVarLen(SerializationUtils.serialize(focSignedVote))
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
            val payload =
                FOCVoteMessage(
                    fileName.toString(Charsets.UTF_8),
                    SerializationUtils.deserialize(mid)
                )
            return Pair(payload, localOffset - offset)
        }
    }
}
