package nl.tudelft.trustchain.detoks

import android.util.Log
import nl.tudelft.trustchain.detoks.gossiper.NetworkSizeGossiper

/**
 * Basic structure for a profile entry
 */
class ProfileEntry(
    val firstSeen: Long = System.currentTimeMillis(),
    var watchTime: Long = 0, // Average watch time
    var duration:  Long = 0, // Video duration
    var hopCount:  Int  = 0, // Amount of other nodes visited
    var timesSeen: Int  = 1  // Count of times we received it
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
    fun updateEntryWatchTime(key: String, time: Long, myUpdate: Boolean) {
        if(!torrents.contains(key)) torrents[key] = ProfileEntry()

        if (myUpdate) {
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
    }

    fun updateEntryHopCount(key: String, hopCount: Int) {
        if(!torrents.contains(key)) torrents[key] = ProfileEntry()
        torrents[key]!!.hopCount = hopCount
    }

    fun incrementTimesSeen(key: String) {
        if(!torrents.contains(key)) torrents[key] = ProfileEntry(timesSeen = 0)
        torrents[key]!!.timesSeen += 1
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
