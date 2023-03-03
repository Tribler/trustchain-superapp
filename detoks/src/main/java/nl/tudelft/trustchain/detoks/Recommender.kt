package nl.tudelft.trustchain.detoks

import kotlin.random.Random

class Recommender {
    private fun coinTossRecommender(uris: Array<String>): String {
        return uris[Random.nextInt(uris.size)]
    }
}
