package nl.tudelft.trustchain.detoks

import com.frostwire.jlibtorrent.TorrentInfo
import nl.tudelft.trustchain.detoks.gossiper.NetworkSizeGossiper
import kotlin.math.min


class ProfileEntry(
    var watched:    Boolean = false,    // True if the video was watched
    var watchTime:  Long = 0,           // Average watch time
    var duration:   Long = 0,           // Video duration
    var uploadDate: Long = 0,           // This is the torrent creation date
    var hopCount:   Int  = 0,           // Amount of other nodes visited
    var timesSeen:  Int  = 0,           // Count of times we received it
    var likes:      Int  = 0,
)

class Profile(
    val profiles: HashMap<String, ProfileEntry> = HashMap()
) {
    object ProfileConfig { const val MAX_DURATION_FACTOR  = 10 }

    fun addProfile(key: String): ProfileEntry {
        if(!profiles.contains(key)) profiles[key] = ProfileEntry(timesSeen = 1)
        return profiles[key]!!
    }

    fun updateEntryDuration(key: String, duration: Long) {
        addProfile(key)
        profiles[key]!!.duration = duration
    }

    fun updateEntryHopCount(key: String, hopCount: Int) {
        addProfile(key)
        profiles[key]!!.hopCount = hopCount
    }

    fun updateEntryLikes(key: String, likes: Int, myUpdate: Boolean) {
        addProfile(key)
        if(myUpdate) {
            profiles[key]!!.likes += (1 / NetworkSizeGossiper.networkSizeEstimate)
        } else {
            profiles[key]!!.likes += likes
            profiles[key]!!.likes /= 2
        }
    }

    fun updateEntryWatchTime(key: String, time: Long, myUpdate: Boolean) {
        addProfile(key)
        if(myUpdate) {
            val newTime = min(time, profiles[key]!!.duration  * ProfileConfig.MAX_DURATION_FACTOR)
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

    fun incrementLikes(key: String) {
        updateEntryLikes(key, likes = 1, myUpdate = true)
    }

    fun incrementTimesSeen(key: String) {
        if(!profiles.contains(key)) profiles[key] = ProfileEntry()
        profiles[key]!!.timesSeen += 1
    }
}
