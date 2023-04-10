package nl.tudelft.trustchain.detoks

import com.frostwire.jlibtorrent.TorrentInfo
import nl.tudelft.trustchain.detoks.gossiper.NetworkSizeGossiper
import java.lang.Long.min


class ProfileEntry(
    val firstSeen:  Long = System.currentTimeMillis(),
    var watched:    Boolean = false,    // True if the video was watched
    var watchTime:  Long = 0,           // Average watch time
    var duration:   Long = 0,           // Video duration
    var uploadDate: Long = 0,           // This is the torrent creation date
    var hopCount:   Int  = 0,           // Amount of other nodes visited
    var timesSeen:  Int  = 1,           // Count of times we received it
    var likes:      Int  = 0,           // TODO: Dependent on other team
) {

}

class Profile(
    val profiles: HashMap<String, ProfileEntry> = HashMap()
) {
    object ProfileConfig { const val MAX_DURATION_RATIO  = 10 }

    fun addProfile(key: String) {
        if(!profiles.contains(key)) profiles[key] = ProfileEntry()
    }

    fun hasWatched(key: String): Boolean {
        return profiles.contains(key) && profiles[key]!!.watched
    }

    fun updateEntryDuration(key: String, duration: Long) {
        addProfile(key)
        profiles[key]!!.duration = duration
    }

    fun updateEntryHopCount(key: String, hopCount: Int) {
        addProfile(key)
        profiles[key]!!.hopCount = hopCount
    }

    fun updateEntryWatchTime(key: String, time: Long, myUpdate: Boolean) {
        addProfile(key)
        if(myUpdate) {
            val newTime = min(time, profiles[key]!!.watchTime  * ProfileConfig.MAX_DURATION_RATIO)
            profiles[key]!!.watchTime += (newTime / NetworkSizeGossiper.networkSizeEstimate)
            profiles[key]!!.watched = true
        } else {
            profiles[key]!!.watchTime += time
            profiles[key]!!.watchTime /= 2
        }
    }

    fun updateEntryUploadDate(key: String, info: TorrentInfo) {
        addProfile(key)
        profiles[key]!!.uploadDate = info.creationDate()
    }

    fun incrementTimesSeen(key: String) {
        if(!profiles.contains(key)) profiles[key] = ProfileEntry(timesSeen = 0)
        profiles[key]!!.timesSeen += 1
    }
}
