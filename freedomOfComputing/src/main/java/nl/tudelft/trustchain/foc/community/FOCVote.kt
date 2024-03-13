package nl.tudelft.trustchain.foc.community

import nl.tudelft.ipv8.messaging.Deserializable
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

enum class VoteType {
    UP,
    DOWN
}

data class FOCVote(val memberId: String, val voteType: VoteType) : Serializable, nl.tudelft.ipv8.messaging.Serializable {
    override fun serialize(): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
        objectOutputStream.writeObject(this)
        objectOutputStream.flush()
        return byteArrayOutputStream.toByteArray()
    }

    @Suppress("UNCHECKED_CAST")
    companion object Deserializer : Deserializable<FOCVote> {
        override fun deserialize(
            buffer: ByteArray,
            offset: Int
        ): Pair<FOCVote, Int> {
            val byteArrayInputStream = ByteArrayInputStream(buffer)
            val objectInputStream = ObjectInputStream(byteArrayInputStream)
            return Pair(objectInputStream.readObject() as FOCVote, buffer.size)
        }
    }
}
