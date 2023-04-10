package nl.tudelft.trustchain.detoks

import android.content.Context
import com.frostwire.jlibtorrent.TorrentInfo
import nl.tudelft.trustchain.detoks.gossiper.NetworkSizeGossiper


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
            profiles[key]!!.watched = true
            profiles[key]!!.watchTime += (time / NetworkSizeGossiper.networkSizeEstimate)
        } else {
            profiles[key]!!.watchTime += time
            profiles[key]!!.watchTime /= 2
        }
    }

    fun updateEntryUploadDate(key: String, info: TorrentInfo) {
        addProfile(key)
        profiles[key]!!.uploadDate = info.creationDate()
    }

    fun incrementHopCount(key: String) {
        addProfile(key)
        profiles[key]!!.hopCount += 1
    }

    fun incrementTimesSeen(key: String) {
        if(!profiles.contains(key)) profiles[key] = ProfileEntry(timesSeen = 0)
        profiles[key]!!.timesSeen += 1
    }
}
