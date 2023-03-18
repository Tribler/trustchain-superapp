package nl.tudelft.trustchain.detoks

import android.util.Log
import nl.tudelft.trustchain.detoks.TorrentManager.*

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

class Profile(val magnets: HashMap<TorrentHandler, ProfileEntry> = HashMap()) {
    fun updateEntryWatchTime(torrent: TorrentHandler, time: Long) {
        if(!magnets.contains(torrent)) magnets[torrent] = ProfileEntry()
        magnets[torrent]!!.watchTime += time
        val name = torrent.torrentName + "[" + torrent.fileName + "]"
        Log.i("DeToks", "Updated watchtime of $name to ${magnets[torrent]!!.watchTime}")
    }
}

class Recommender {
    private fun coinTossRecommender(magnets: HashMap<TorrentHandler, ProfileEntry>) : Map<TorrentHandler, ProfileEntry> {
        return magnets.map { it.key to it.value }.shuffled().toMap()
    }

    private fun watchTimeRecommender(magnets: HashMap<TorrentHandler, ProfileEntry>) : Map<TorrentHandler, ProfileEntry> {
        return magnets.toList().sortedBy { (_, entry) -> entry }.toMap()
    }
}
