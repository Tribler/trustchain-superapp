package nl.tudelft.trustchain.musicdao.core.recommender.model

abstract class NodeOrSong(
    val identifier: String,
    var rankingScore: Double
) {

    override fun toString(): String {
        return "identifier: $identifier score: $rankingScore"
    }

    override fun hashCode(): Int {
        return identifier.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is NodeOrSong && identifier == other.identifier
    }
}
