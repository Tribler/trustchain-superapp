package nl.tudelft.trustchain.detoks

import android.util.Log
import nl.tudelft.trustchain.detoks.gossiper.NetworkSizeGossiper

/**
 * Basic structure for a profile entry
 */
class ProfileEntry(
    var watchTime: Long = 0, // Average watch time
    val firstSeen: Long = System.currentTimeMillis()
)

class Profile(
    val profiles: HashMap<String, ProfileEntry> = HashMap()
) {
    fun updateEntryWatchTime(key: String, time: Long, myUpdate: Boolean) {
        if (!profiles.contains(key)) profiles[key] = ProfileEntry()

        if (myUpdate) {
            profiles[key]!!.watchTime += (time / NetworkSizeGossiper.networkSizeEstimate)
        } else {
            profiles[key]!!.watchTime += time
            profiles[key]!!.watchTime /= 2
        }
        Log.i(
            DeToksCommunity.LOGGING_TAG,
            "Updated watchtime of $key to ${profiles[key]!!.watchTime}"
        )
    }
}
