package nl.tudelft.trustchain.detoks

import android.util.Log
import nl.tudelft.trustchain.detoks.TorrentManager.TorrentHandler

class Strategy {

    val strategyComparators = mutableMapOf<Int, (
        Pair<TorrentHandler, ProfileEntry?>,
        Pair<TorrentHandler, ProfileEntry?>
    ) -> Int>()

    val leachingStrategy = STRATEGY_RANDOM
    val seedingStrategy = STRATEGY_RANDOM

    companion object {
        const val STRATEGY_RANDOM = 0
        const val STRATEGY_HIGHEST_WATCH_TIME = 1
        const val STRATEGY_LOWEST_WATCH_TIME = 2
    }

    init {
        strategyComparators[STRATEGY_HIGHEST_WATCH_TIME] = :: highestWatchTimeStrategy
        strategyComparators[STRATEGY_LOWEST_WATCH_TIME] = :: lowestWatchTimeStrategy
    }

    /**
     * Returns the torrent handlers based on decreasing watch time.
     */
    private fun highestWatchTimeStrategy(
        p0: Pair<TorrentHandler, ProfileEntry?>,
        p1: Pair<TorrentHandler, ProfileEntry?>
    ) : Int = p0.second!!.watchTime compareTo p1.second!!.watchTime

    /**
     * Returns the torrent handlers based on increasing watch time.
     */
    private fun lowestWatchTimeStrategy(
        p0: Pair<TorrentHandler, ProfileEntry?>,
        p1: Pair<TorrentHandler, ProfileEntry?>
    ) : Int = p1.second!!.watchTime compareTo p0.second!!.watchTime

    /**
     * Applies the sorting function sent as parameter, to the handlers, based on the profiles.
     */
    private fun applyStrategy(
        handlers: MutableList<TorrentHandler>,
        profiles: HashMap<String, ProfileEntry>,
        func: (
            Pair<TorrentHandler, ProfileEntry?>,
            Pair<TorrentHandler, ProfileEntry?>
        ) -> Int
    ): MutableList<TorrentHandler> {
        val handlerProfile = handlers.map {
            val key = it.handle.infoHash().toString()
            if (!profiles.contains(key)) Pair(it, ProfileEntry())
            Pair(it, profiles[key])
        }

        val sortedHandlerProfile = handlerProfile.sortedWith(func)
        return sortedHandlerProfile.map { it.first }.toMutableList()
    }

    fun changeStrategy(item: Int) {
        Log.i(DeToksCommunity.LOGGING_TAG, item.toString())
    }
}
