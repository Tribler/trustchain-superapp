package nl.tudelft.trustchain.musicdao.core.recommender.model
class Node(
    key: String,
    personalisedPageRankScore: Double = 0.0
) : NodeOrSong(key, personalisedPageRankScore) {

    fun getKey(): String {
        return identifier
    }

    fun getPersonalizedPageRankScore(): Double {
        return rankingScore
    }

    fun setPersonalizedPageRankScore(score: Double) {
        rankingScore = score
    }

    override fun hashCode(): Int {
        return identifier.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is Node && identifier == other.identifier
    }
}
