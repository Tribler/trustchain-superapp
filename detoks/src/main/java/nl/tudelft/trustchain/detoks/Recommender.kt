package nl.tudelft.trustchain.detoks

import android.util.Log
import kotlin.random.Random

// TODO: On downloading a new torrent, check all the other profiles to rank it
// TODO: On sending a video/peer discovery, attach the profile to keep peers updated
// TODO: Generate 100 dummy profiles with dummy entries to do testing with
// TODO: Compare the performance of coin toss against watch time

/**
 * Basic structure for a profile entry
 * Note: Both the duration and freshness are currently not used
 */
class ProfileEntry(
    var duration: Long = 0,
    var watchTime: Long = 0,
    val freshness: Long = 0
)

class Profile(
    val magnets: HashMap<String, ProfileEntry> = HashMap()
) {
    fun updateEntryWatchTime(torrent: TorrentManager.TorrentHandler, time: Long) {
        val name = torrent.torrentName + "[" + torrent.fileName + "]"
        if(!magnets.contains(name)) magnets[name] =
            ProfileEntry(0, time, System.currentTimeMillis())
        magnets[name]!!.watchTime += time
        Log.i("DeToks", "Updated watchtime of $name to ${magnets[name]!!.watchTime}")
    }

    /**
     * Update the entry duration in the profile
     * NOTE: This function is currently not used
     */
    fun updateEntryDuration(torrent: TorrentManager.TorrentHandler, duration: Long) {
        val name = torrent.torrentName + "[" + torrent.fileName + "]"
        if(!magnets.contains(name)) magnets[name] =
            ProfileEntry(duration, 0, System.currentTimeMillis())
        else magnets[name]!!.duration = duration
        Log.i("DeToks", "Updated duration of $name to ${magnets[name]!!.duration}")
    }
}

class Recommender {
    private fun coinTossRecommender(URIs: List<String>): String {
        return URIs[Random.nextInt(URIs.size)]
    }

    // TODO: Return a sorted list of videos not just the 'winner'
    private fun watchTimeDurationRatioRecommender(
        URIs: List<String>,
        profiles: List<Profile>
    ): String {
        var (candidate, averagePercentageWatched) = Pair("", 0.0f)
        for (URI: String in URIs) {
            var (hits, percentageWatchedTotal) = Pair(0.0f, 0.0f)
            for (profile: Profile in profiles) {
                if (profile.magnets.containsKey(URI)) {
                    val entry = profile.magnets[URI]
                    percentageWatchedTotal += 100.0f * (entry!!.watchTime / entry.duration)
                    hits += 1.0f
                }
            }
            if (percentageWatchedTotal / hits > averagePercentageWatched) {
                averagePercentageWatched = percentageWatchedTotal / hits
                candidate = URI
            }
        }
        return candidate
    }

    // TODO: Implement a function which returns a sorted list of videos with the highest watch time
    // private fun watchTimeRecommender(URIs: List<String>, profiles: List<Profile>): String { }
}
