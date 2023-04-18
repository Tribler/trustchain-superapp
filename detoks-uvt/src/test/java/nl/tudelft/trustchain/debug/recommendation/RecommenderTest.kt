package nl.tudelft.trustchain.debug.recommendation

import io.mockk.every
import io.mockk.mockkObject
import nl.tudelft.trustchain.detoks.recommendation.RecommendationType
import nl.tudelft.trustchain.detoks.recommendation.Recommender
import org.junit.Test

class RecommenderTest {
    @Test
    fun addRecommendationTest() {
        val nextRecommendation: String = "Should be added once"
        val nextRecommendations: ArrayList<String> = ArrayList()
        nextRecommendations.add(nextRecommendation)

        Recommender.addRecommendations(nextRecommendations, RecommendationType.RANDOM)


    }
}
