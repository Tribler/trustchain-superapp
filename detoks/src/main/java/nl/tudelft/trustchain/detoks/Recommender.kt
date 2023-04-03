package nl.tudelft.trustchain.detoks

import android.util.Log
import nl.tudelft.trustchain.detoks.gossiper.NetworkSizeGossiper

/**
 * Basic structure for a profile entry
 */
class ProfileEntry(
    var watched:    Boolean = false,
    val firstSeen:  Long = System.currentTimeMillis(),
    var uploadDate: Long = 0, // This is the torrent creation date
    var watchTime:  Long = 0, // Average watch time
    var duration:   Long = 0, // Video duration
    var hopCount:   Int  = 0, // Amount of other nodes visited
    var timesSeen:  Int  = 1, // Count of times we received it
    var likes:      Int  = 0  // Unimplemented
) : Comparable<ProfileEntry> {
    override fun compareTo(other: ProfileEntry): Int = when {
        this.watchTime != other.watchTime -> this.watchTime compareTo other.watchTime
        this.firstSeen != other.firstSeen -> this.firstSeen compareTo other.firstSeen
        else -> 0
    }
}

class Profile(
    val torrents: HashMap<String, ProfileEntry> = HashMap()
) {
    fun hasWatched(key: String): Boolean {
        return torrents.contains(key) && torrents[key]!!.watched
    }

    fun updateEntryWatchTime(key: String, time: Long, myUpdate: Boolean) {
        if(!torrents.contains(key)) torrents[key] = ProfileEntry()

        if (myUpdate) {
            torrents[key]!!.watched = true
            torrents[key]!!.watchTime += (time / NetworkSizeGossiper.networkSizeEstimate)
        } else {
            torrents[key]!!.watchTime += time
            torrents[key]!!.watchTime /= 2
        }
        Log.i(DeToksCommunity.LOGGING_TAG, "Updated watchtime of $key to ${torrents[key]!!.watchTime}")
    }

    fun updateEntryDuration(key: String, duration: Long) {
        if(!torrents.contains(key)) torrents[key] = ProfileEntry()
        torrents[key]!!.duration = duration
        Log.i(DeToksCommunity.LOGGING_TAG, "Updated duration of $key to ${torrents[key]!!.duration}")
    }

    fun updateEntryHopCount(key: String, hopCount: Int) {
        if(!torrents.contains(key)) torrents[key] = ProfileEntry()
        torrents[key]!!.hopCount = hopCount
        Log.i(DeToksCommunity.LOGGING_TAG, "Updated hop count of $key to ${torrents[key]!!.hopCount}")
    }

    fun incrementTimesSeen(key: String) {
        if(!torrents.contains(key)) torrents[key] = ProfileEntry(timesSeen = 0)
        torrents[key]!!.timesSeen += 1
        Log.i(DeToksCommunity.LOGGING_TAG, "Updated times seen of $key to ${torrents[key]!!.timesSeen}")
    }
}

class Recommender {
    private fun coinTossRecommender(torrents: HashMap<String, ProfileEntry>): Map<String, ProfileEntry> {
        return torrents.map { it.key to it.value }.shuffled().toMap()
    }

    private fun watchTimeRecommender(torrents: HashMap<String, ProfileEntry>): Map<String, ProfileEntry> {
        return torrents.toList().sortedBy { (_, entry) -> entry }.toMap()
    }
}
