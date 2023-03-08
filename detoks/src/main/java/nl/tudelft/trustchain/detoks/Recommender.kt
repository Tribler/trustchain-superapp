package nl.tudelft.trustchain.detoks

import kotlin.random.Random

// TODO: Implement some kind of handler for all of this that also stores the user's own profile
// TODO: On downloading a new torrent, check all the other profiles to rank it
// TODO: On sending a video/peer discovery, attach the profile to keep peers updated
// TODO: After watching a video, update the (new) hashmap entry
// TODO: Generate 100 dummy profiles with dummy entries to do testing with
// TODO: Compare the performance of coin toss against watch time

class ProfileEntry(
    val duration: Int,      // TODO: Change to time
    val watchTime: Int,     // TODO: Change to time
    val freshness: Double,
)

class Profile(
    val magnets: HashMap<String, ProfileEntry>,
)

class Recommender {
    private fun coinTossRecommender(URIs: List<String>): String {
        return URIs[Random.nextInt(URIs.size)]
    }

    // TODO: Return a sorted list of videos not just the 'winner'
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
