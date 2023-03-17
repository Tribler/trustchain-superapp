package nl.tudelft.trustchain.detoks

import android.util.Log
import kotlin.random.Random

/**
 * Basic structure for a profile entry
 */
class ProfileEntry(
    var watchTime: Long = 0,
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
    fun updateEntryWatchTime(torrent: TorrentManager.TorrentHandler, time: Long) {
        val name = torrent.torrentName + "[" + torrent.fileName + "]"
        if(!magnets.contains(name)) magnets[name] = ProfileEntry()
        magnets[name]!!.watchTime += time
        Log.i("DeToks", "Updated watchtime of $name to ${magnets[name]!!.watchTime}")
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
