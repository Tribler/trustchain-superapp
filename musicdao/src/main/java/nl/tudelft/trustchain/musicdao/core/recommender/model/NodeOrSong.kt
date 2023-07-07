package nl.tudelft.trustchain.musicdao.core.recommender.model

import kotlinx.serialization.Serializable

@Serializable
abstract class NodeOrSong(
    val identifier: String,
    var rankingScore: Double
) {

    override fun toString(): String {
        return "${if (this is Node) "Node:" else "SongRec:"} $identifier"
    }

    private fun hashCodeString(): String {
        return "${if (this is Node) "n" else "s"}$identifier"
    }

    override fun hashCode(): Int {
        return hashCodeString().hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this is Node) {
            return other is Node && identifier == other.identifier
        } else {
            return other is Recommendation && identifier == other.identifier
        }
    }
}
