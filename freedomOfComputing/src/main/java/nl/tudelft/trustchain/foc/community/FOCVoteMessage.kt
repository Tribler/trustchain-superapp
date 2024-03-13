package nl.tudelft.trustchain.foc.community

import nl.tudelft.ipv8.messaging.Deserializable
import java.io.Serializable

data class FOCVoteMessage(val fileName: String, val focVote: FOCVote) : Serializable, nl.tudelft.ipv8.messaging.Serializable {
    override fun serialize(): ByteArray {
        return this.serialize()
    }

    companion object Deserializer : Deserializable<FOCVoteMessage> {
        override fun deserialize(
            buffer: ByteArray,
            offset: Int
        ): Pair<FOCVoteMessage, Int> {
            return this.deserialize(buffer)
        }
    }
}
