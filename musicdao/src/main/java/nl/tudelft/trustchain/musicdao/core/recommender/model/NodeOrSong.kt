package nl.tudelft.trustchain.musicdao.core.recommender.model

import nl.tudelft.trustchain.musicdao.core.repositories.model.Song

abstract class NodeOrSong(
    val identifier: String,
    var rankingScore: Double
) {

    override fun toString(): String {
        return "${if(this is Node) "Node:" else "SongRec:"} $identifier"
    }

    private fun hashCodeString(): String {
        return "${if(this is Node) "n" else "s"}$identifier"
    }

    override fun hashCode(): Int {
        return hashCodeString().hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if(this is Node) {
            return other is Node && identifier == other.identifier
        } else {
            return other is SongRecommendation && identifier == other.identifier
        }
    }
}
