package nl.tudelft.trustchain.detoks

import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable

private const val DELIMITER = "|"

// TODO: also add torrent name and video creator
class Like(val liker: String, val video: String) : Serializable {
    // TODO: verify liker and video don't have the delimiter in them, or choose different one
    override fun serialize(): ByteArray {
        return (liker + DELIMITER + video).toByteArray()
    }

    companion object Deserializer : Deserializable<Like> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<Like, Int> {
            val str = buffer.toString(Charsets.UTF_8).split("|")
            val like = Like(str[0], str[1])
            return Pair(like, buffer.size)
        }
    }
}
