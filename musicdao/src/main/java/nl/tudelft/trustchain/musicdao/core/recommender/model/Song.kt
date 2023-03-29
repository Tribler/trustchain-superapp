package nl.tudelft.trustchain.musicdao.core.recommender.model

import kotlinx.serialization.Serializable
@Serializable
data class
Song(
    val torrentHash: String
):NodeOrSong(identifier = torrentHash) {

    override fun hashCode(): Int {
        return toString().hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is Song && toString() == other.toString()
    }
}
