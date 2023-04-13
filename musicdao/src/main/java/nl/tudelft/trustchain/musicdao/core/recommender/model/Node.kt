package nl.tudelft.trustchain.musicdao.core.recommender.model

class Node(
    ipv8: String,
    personalisedPageRankScore: Double = 0.0
):NodeOrSong(ipv8, personalisedPageRankScore) {

    fun getIpv8(): String {
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
