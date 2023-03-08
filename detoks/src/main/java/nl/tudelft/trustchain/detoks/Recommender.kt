package nl.tudelft.trustchain.detoks

import kotlin.random.Random

class ProfileEntry(
    val duration: Int,
    val watchTime: Int,
    val freshness: Double,
)

class Profile(
    val magnets: HashMap<String, ProfileEntry>,
)

class Recommender {
    private fun coinTossRecommender(URIs: List<String>): String {
        return URIs[Random.nextInt(URIs.size)]
    }

    private fun watchTimeRecommender(URIs: List<String>, profiles: List<Profile>): String {
        var (candidate, averagePercentageWatched) = Pair("", 0.0f)
        for(URI: String in URIs) {
            var (hits, percentageWatchedTotal) = Pair(0.0f, 0.0f)
            for(profile: Profile in profiles) {
                if(profile.magnets.containsKey(URI)) {
                    val entry = profile.magnets[URI]
                    percentageWatchedTotal += 100.0f * (entry!!.watchTime / entry.duration)
                    hits += 1.0f
                }
            }
            if(percentageWatchedTotal / hits > averagePercentageWatched) {
                averagePercentageWatched = percentageWatchedTotal / hits
                candidate = URI
            }
        }
        return candidate
    }
}
