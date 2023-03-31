package nl.tudelft.trustchain.detoks

import android.util.Log
import nl.tudelft.trustchain.detoks.TorrentManager.TorrentHandler

class Strategy {

    val strategyComparators = mutableMapOf<Int, Comparator<Pair<TorrentHandler, ProfileEntry?>>>()
//private val strategyComparators = mutableMapOf<Int, (Pair<TorrentHandler, ProfileEntry?>, Pair<TorrentHandler, ProfileEntry?>) -> Int>()

    var leachingStrategy = STRATEGY_RANDOM
    var seedingStrategy = STRATEGY_RANDOM

    companion object {
        const val STRATEGY_RANDOM = 0
        const val STRATEGY_HIGHEST_WATCH_TIME = 1
        const val STRATEGY_LOWEST_WATCH_TIME = 2
    }

    init {
        strategyComparators[STRATEGY_HIGHEST_WATCH_TIME] = Comparator { p0, p1 -> p0?.second!!.watchTime compareTo p1?.second!!.watchTime }
        strategyComparators[STRATEGY_LOWEST_WATCH_TIME] = Comparator { p0, p1 -> p1.second!!.watchTime compareTo p0.second!!.watchTime }

//        strategyComparators[STRATEGY_HIGHEST_WATCH_TIME] = highestWatchTimeStrategy
//        strategyComparators[STRATEGY_LOWEST_WATCH_TIME] = :: lowestWatchTimeStrategy
    }

//    /**
//     * Returns the torrent handlers based on decreasing watch time.
//     */
//    private fun highestWatchTimeStrategy() : Comparator<Pair<TorrentHandler, ProfileEntry?>> = Comparator { p0, p1 -> p0?.second!!.watchTime compareTo p1?.second!!.watchTime }
//
//
//    /**
//     * Returns the torrent handlers based on increasing watch time.
//     */
//    private fun lowestWatchTimeStrategy(
//        p0: Pair<TorrentHandler, ProfileEntry?>,
//        p1: Pair<TorrentHandler, ProfileEntry?>
//    ) : Int = p1.second!!.watchTime compareTo p0.second!!.watchTime


    /**
     * Applies the sorting function sent as parameter, to the handlers, based on the profiles.
     */
    internal fun applyStrategy(
        id: Int,
        handlers: MutableList<TorrentHandler>,
        profiles: HashMap<String, ProfileEntry>
    ): MutableList<TorrentHandler> {
        if (id == 0) return handlers.shuffled().toMutableList()
        if (strategyComparators[id] == null) return handlers

        val handlerProfile = handlers.map {
            val key = it.handle.infoHash().toString()
            if (!profiles.contains(key)) Pair(it, ProfileEntry())
            Pair(it, profiles[key])
        }
        val sortedHandlerProfile = handlerProfile.sortedWith(strategyComparators[id]!!).toMutableList()

        return sortedHandlerProfile.map { it.first }.toMutableList()
    }
}
