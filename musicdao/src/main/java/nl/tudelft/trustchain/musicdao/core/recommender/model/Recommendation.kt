package nl.tudelft.trustchain.musicdao.core.recommender.model

class Recommendation(
    uniqueIdentifier: String,
    recommendationScore: Double = 0.0
):NodeOrSong(uniqueIdentifier, recommendationScore) {

    fun getUniqueIdentifier(): String {
        return identifier
    }

    fun getRecommendationScore(): Double {
        return rankingScore
    }

    fun setRecommendationScore(score: Double) {
        rankingScore = score
    }

    override fun hashCode(): Int {
        return identifier.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is Recommendation && identifier == other.identifier
    }
}
