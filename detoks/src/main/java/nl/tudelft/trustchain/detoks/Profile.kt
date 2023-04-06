package nl.tudelft.trustchain.detoks

import android.util.Log
import nl.tudelft.trustchain.detoks.gossiper.NetworkSizeGossiper


class ProfileEntry(
    var watched:    Boolean = false,
    val firstSeen:  Long = System.currentTimeMillis(),
    var uploadDate: Long = 0, // This is the torrent creation date
    var watchTime:  Long = 0, // Average watch time
    var duration:   Long = 0, // Video duration
    var hopCount:   Int  = 0, // Amount of other nodes visited
    var timesSeen:  Int  = 1, // Count of times we received it
    var likes:      Int  = 0  // TODO: Dependent on other team
)

class Profile(
    val profiles: HashMap<String, ProfileEntry> = HashMap()
) {
    fun hasWatched(key: String): Boolean {
        return profiles.contains(key) && profiles[key]!!.watched
    }

    fun updateEntryWatchTime(key: String, time: Long, myUpdate: Boolean) {
        if(!profiles.contains(key)) profiles[key] = ProfileEntry()

        if (myUpdate) {
            profiles[key]!!.watched = true
            profiles[key]!!.watchTime += (time / NetworkSizeGossiper.networkSizeEstimate)
        } else {
            profiles[key]!!.watchTime += time
            profiles[key]!!.watchTime /= 2
        }
        Log.i(DeToksCommunity.LOGGING_TAG, "Updated watchtime of $key to ${profiles[key]!!.watchTime}")
    }

    fun updateEntryDuration(key: String, duration: Long) {
        if(!profiles.contains(key)) profiles[key] = ProfileEntry()
        profiles[key]!!.duration = duration
        Log.i(DeToksCommunity.LOGGING_TAG, "Updated duration of $key to ${profiles[key]!!.duration}")
    }

    fun updateEntryHopCount(key: String, hopCount: Int) {
        if(!profiles.contains(key)) profiles[key] = ProfileEntry()
        profiles[key]!!.hopCount = hopCount
        Log.i(DeToksCommunity.LOGGING_TAG, "Updated hop count of $key to ${profiles[key]!!.hopCount}")
    }

    fun incrementTimesSeen(key: String) {
        if(!profiles.contains(key)) profiles[key] = ProfileEntry(timesSeen = 0)
        profiles[key]!!.timesSeen += 1
        Log.i(DeToksCommunity.LOGGING_TAG, "Updated times seen of $key to ${profiles[key]!!.timesSeen}")
    }
}
