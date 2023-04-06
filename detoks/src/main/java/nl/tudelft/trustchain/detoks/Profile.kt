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
    private fun log(string: String) {
        Log.i(DeToksCommunity.LOGGING_TAG, string)
    }

    fun hasWatched(key: String): Boolean {
        return profiles.contains(key) && profiles[key]!!.watched
    }

    fun updateEntryWatchTime(key: String, time: Long, myUpdate: Boolean, log: Boolean = false) {
        if(!profiles.contains(key)) profiles[key] = ProfileEntry()

        if(myUpdate) {
            if(!profiles[key]!!.watched && log) log("Watched $key for the first time")
            profiles[key]!!.watched = true
            profiles[key]!!.watchTime += (time / NetworkSizeGossiper.networkSizeEstimate)
        } else {
            profiles[key]!!.watchTime += time
            profiles[key]!!.watchTime /= 2
        }
        if(log) log("Updated watchtime of $key to ${profiles[key]!!.watchTime}")
    }

    fun updateEntryDuration(key: String, duration: Long, log: Boolean = false) {
        if(!profiles.contains(key)) profiles[key] = ProfileEntry()
        profiles[key]!!.duration = duration
        if(log) log("Updated duration of $key to ${profiles[key]!!.duration}")
    }

    fun updateEntryHopCount(key: String, hopCount: Int, log: Boolean = false) {
        if(!profiles.contains(key)) profiles[key] = ProfileEntry()
        profiles[key]!!.hopCount = hopCount
        if(log) log("Updated hop count of $key to ${profiles[key]!!.hopCount}")
    }

    fun incrementTimesSeen(key: String, log: Boolean = false) {
        if(!profiles.contains(key)) profiles[key] = ProfileEntry(timesSeen = 0)
        profiles[key]!!.timesSeen += 1
        if(log) log("Updated times seen of $key to ${profiles[key]!!.timesSeen}")
    }
}
