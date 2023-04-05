package nl.tudelft.trustchain.detoks

import android.util.Log
import nl.tudelft.trustchain.detoks.gossiper.NetworkSizeGossiper
import java.lang.Long.min


class ProfileEntry(
    var duration:  Long = 0, // Video duration
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
    val torrents: HashMap<String, ProfileEntry> = HashMap()
) {
    object ProfileConfig { const val MAX_DURATION_RATIO  = 10 }

    fun updateEntryWatchTime(key: String, time: Long, myUpdate: Boolean) {
        if(!torrents.contains(key)) torrents[key] = ProfileEntry()

        if(myUpdate) {
            val newTime = min(time, torrents[key]!!.watchTime  * ProfileConfig.MAX_DURATION_RATIO)
            torrents[key]!!.watchTime += (newTime / NetworkSizeGossiper.networkSizeEstimate)
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
}
