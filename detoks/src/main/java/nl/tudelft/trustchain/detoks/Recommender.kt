package nl.tudelft.trustchain.detoks

import android.util.Log
import nl.tudelft.trustchain.detoks.gossiper.NetworkSizeGossiper

/**
 * Basic structure for a profile entry
 */
class ProfileEntry(
    var watchTime: Long = 0, // Average watch time
    val firstSeen: Long = System.currentTimeMillis()
) : Comparable<ProfileEntry> {
    override fun compareTo(other: ProfileEntry): Int = when {
        this.watchTime != other.watchTime -> this.watchTime compareTo other.watchTime
        this.firstSeen != other.firstSeen -> this.firstSeen compareTo other.firstSeen
        else -> 0
    }
}

class Profile(
    val magnets: HashMap<String, ProfileEntry> = HashMap()
) {
    fun updateEntryWatchTime(key: String, time: Long, myUpdate: Boolean) {
        if(!magnets.contains(key)) magnets[key] = ProfileEntry()

        if (myUpdate) {
            magnets[key]!!.watchTime += (time / NetworkSizeGossiper.networkSizeEstimate)
        } else {
            magnets[key]!!.watchTime += time
            magnets[key]!!.watchTime /= 2
        }
        Log.i(DeToksCommunity.LOGGING_TAG, "Updated watchtime of $key to ${magnets[key]!!.watchTime}")
    }
}

class Recommender {
    private fun coinTossRecommender(magnets: HashMap<String, ProfileEntry>): Map<String, ProfileEntry> {
        return magnets.map { it.key to it.value }.shuffled().toMap()
    }

    private fun watchTimeRecommender(magnets: HashMap<String, ProfileEntry>): Map<String, ProfileEntry> {
        return magnets.toList().sortedBy { (_, entry) -> entry }.toMap()
    }
}
