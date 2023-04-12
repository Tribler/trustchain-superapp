package nl.tudelft.trustchain.detoks

import nl.tudelft.trustchain.detoks.TorrentManager.TorrentHandler

class Strategy {

private val strategyComparators = mutableMapOf<Int, (Pair<TorrentHandler, ProfileEntry?>, Pair<TorrentHandler, ProfileEntry?>) -> Int>()

    var isSeeding = false

    var leechingStrategy = STRATEGY_RANDOM
    var seedingStrategy = STRATEGY_RANDOM

    var seedingBandwidthLimit = 0
    var storageLimit : Int = 0

    companion object {

        const val STRATEGY_RANDOM = 0
        const val STRATEGY_HIGHEST_WATCH_TIME = 1
        const val STRATEGY_LOWEST_WATCH_TIME = 2
        const val STRATEGY_HOT = 3
        const val STRATEGY_RISING = 4
        const val STRATEGY_NEW = 5
        const val STRATEGY_TOP = 6
    }

    init {
        strategyComparators[STRATEGY_HIGHEST_WATCH_TIME] = :: highestWatchTimeStrategy
        strategyComparators[STRATEGY_LOWEST_WATCH_TIME] = :: lowestWatchTimeStrategy

        strategyComparators[STRATEGY_HOT] = :: lowestWatchTimeStrategy
        strategyComparators[STRATEGY_RISING] = :: lowestWatchTimeStrategy
        strategyComparators[STRATEGY_NEW] = :: lowestWatchTimeStrategy
        strategyComparators[STRATEGY_TOP] = :: lowestWatchTimeStrategy
    }

    /**
     * Returns the torrent handlers based on decreasing watch time.
     */
    private fun highestWatchTimeStrategy(
        p0: Pair<TorrentHandler, ProfileEntry?>,
        p1: Pair<TorrentHandler, ProfileEntry?>
    ) : Int = p1.second!!.watchTime compareTo p0.second!!.watchTime

    /**
     * Returns the torrent handlers based on increasing watch time.
     */
    private fun lowestWatchTimeStrategy(
        p0: Pair<TorrentHandler, ProfileEntry?>,
        p1: Pair<TorrentHandler, ProfileEntry?>
    ) : Int = p1.second!!.watchTime compareTo p0.second!!.watchTime

    /**
     * Returns the torrent handlers based on when they were first uploaded
     */
    //TODO: add the new strategies

    /**
     * Applies the sorting function sent as parameter, to the handlers, based on the profiles.
     */
    internal fun applyStrategy(
        id: Int,
        handlers: MutableList<TorrentHandler>,
        profiles: HashMap<String, ProfileEntry>
    ): MutableList<TorrentHandler> {
        if (id == 0) return handlers.shuffled().toMutableList()
        if (!strategyComparators.contains(id)) return handlers

        val handlerProfile = handlers.map {
            val key = it.handle.infoHash().toString()
            if (!profiles.contains(key)) return@map Pair(it, ProfileEntry())
            return@map Pair(it, profiles[key])
        }
        val sortedHandlerProfile =
            handlerProfile.sortedWith(strategyComparators[id]!!).toMutableList()

        return sortedHandlerProfile.map { it.first }.toMutableList()
    }


}
