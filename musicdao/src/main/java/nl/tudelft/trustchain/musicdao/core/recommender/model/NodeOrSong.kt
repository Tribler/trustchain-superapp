package nl.tudelft.trustchain.musicdao.core.recommender.model

import kotlinx.serialization.Serializable
@Serializable
open class
NodeOrSong(
    val identifier: String
) {

    override fun hashCode(): Int {
        return toString().hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is NodeOrSong && toString() == other.toString()
    }
}
