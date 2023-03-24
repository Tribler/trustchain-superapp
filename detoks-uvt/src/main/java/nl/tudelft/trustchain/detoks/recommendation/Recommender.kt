package nl.tudelft.trustchain.detoks.recommendation

import android.util.Log

class Recommender {
    companion object {
        private var recommendations = mutableListOf<String>()

        fun addRecommendation(videoId: String) {
            recommendations.add(videoId)
        }

        fun createNewRecommendations() {
            Log.i("DeToks", "Creating new recommendations...")
        }

        fun getNextRecommendation(): String {
            if (recommendations.size > 0) {
                val firstVideoID = recommendations.first()
                recommendations.remove(firstVideoID)
                return firstVideoID
            } else {
                createNewRecommendations()
                return getNextRecommendation()
            }
        }
    }
}
