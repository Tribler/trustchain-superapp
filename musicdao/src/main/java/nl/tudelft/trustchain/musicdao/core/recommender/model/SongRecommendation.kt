package nl.tudelft.trustchain.musicdao.core.recommender.model

class SongRecommendation(
    torrentHash: String,
    recommendationScore: Double = 0.0
):NodeOrSong(torrentHash, recommendationScore) {

    fun getTorrentHash(): String {
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
        return other is SongRecommendation && identifier == other.identifier
    }
}
