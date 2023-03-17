package nl.tudelft.trustchain.detoks

import nl.tudelft.ipv8.attestation.trustchain.TransactionEncoding
import nl.tudelft.ipv8.messaging.*

//private const val DELIMITER = "|"


class Like(val liker: String, val video: String, val torrent: String, val creator_name: String) : Serializable {
    fun toMap(): Map<String, String> {
        return mapOf(
            "liker" to liker,
            "video" to video,
            "torrent" to torrent,
            "creator_name" to creator_name,
        )
    }
    override fun serialize(): ByteArray {
        return serializeVarLen(liker.toByteArray(Charsets.UTF_8)) +
            serializeVarLen(video.toByteArray(Charsets.UTF_8)) +
            serializeVarLen(torrent.toByteArray(Charsets.UTF_8)) +
            serializeVarLen(creator_name.toByteArray(Charsets.UTF_8))
    }

    companion object Deserializer : Deserializable<Like> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<Like, Int> {
            var localOffset = 0
            val (liker, likerSize) = deserializeVarLen(buffer, offset + localOffset)
            localOffset += likerSize
            val (video, videoSize) = deserializeVarLen(buffer, offset + localOffset)
            localOffset += videoSize
            val (torrent, torrentSize) = deserializeVarLen(buffer, offset + localOffset)
            localOffset += torrentSize
            val (creator_name, cretorSize) = deserializeVarLen(buffer, offset + localOffset)
            localOffset += cretorSize
            return Pair(
                Like(
                    liker.toString(Charsets.UTF_8),
                    video.toString(Charsets.UTF_8),
                    torrent.toString(Charsets.UTF_8),
                    creator_name.toString(Charsets.UTF_8)
                ),
                localOffset
            )
        }
    }
}
