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
        const val STRATEGY_HOPCOUNT = 7

        const val RISING_CUTOFF_SECONDS = 7200 // 2 hour cutoff
        const val HOT_CUTOFF_SECONDS = 24 * 3600//  1 day cutoff
    }

    init {
        strategyComparators[STRATEGY_HIGHEST_WATCH_TIME] = :: highestWatchTimeStrategy
        strategyComparators[STRATEGY_LOWEST_WATCH_TIME] = :: lowestWatchTimeStrategy

        strategyComparators[STRATEGY_HOT] = :: highestWatchTimeStrategy
        strategyComparators[STRATEGY_RISING] = :: highestWatchTimeStrategy
        strategyComparators[STRATEGY_NEW] = :: newestFirstStrategy
        strategyComparators[STRATEGY_TOP] = :: topFirstStrategy
        strategyComparators[STRATEGY_HOPCOUNT] = :: hopCountStrategy
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
    ) : Int = p0.second!!.watchTime compareTo p1.second!!.watchTime

    /**
     * Returns the torrent handlers based on number of likes
     */
    private fun topFirstStrategy(
        p0: Pair<TorrentHandler, ProfileEntry?>,
        p1: Pair<TorrentHandler, ProfileEntry?>
    ) : Int = p0.second!!.likes compareTo p1.second!!.likes

    /**
     * Returns the torrent handlers based on when they were first uploaded
     */
    private fun newestFirstStrategy(
        p0: Pair<TorrentHandler, ProfileEntry?>,
        p1: Pair<TorrentHandler, ProfileEntry?>
    ) : Int = p0.second!!.uploadDate compareTo p1.second!!.uploadDate

    /**
     * Returns the torrent handlers based on hopcount
     */
    private fun hopCountStrategy(
        p0: Pair<TorrentHandler, ProfileEntry?>,
        p1: Pair<TorrentHandler, ProfileEntry?>
    ) : Int = p0.second!!.hopCount compareTo p1.second!!.hopCount

    /**
     * Determines if a torrent should be high in the list by checking the upload
     * time to the current time using a given cutoff time
     */
    private fun cutoffComparator(
        item: Pair<TorrentHandler, ProfileEntry?>,
        cutoff: Int

    ): Boolean {
        val currentTime = System.currentTimeMillis() / 1000;
        val minimumDate = currentTime - cutoff

        return item.second!!.uploadDate >= minimumDate
    }

    /**
     * Applies the sorting function sent as parameter, to the handlers, based on the profiles.
     */
    internal fun applyStrategy(
        id: Int,
        handlers: MutableList<TorrentHandler>,
        profiles: HashMap<String, ProfileEntry>
    ): MutableList<TorrentHandler> {
        if (id == STRATEGY_RANDOM) return handlers.shuffled().toMutableList()
        if (!strategyComparators.contains(id)) return handlers

        var handlerProfile = handlers.map {
            val key = it.handle.infoHash().toString()
            if (!profiles.contains(key)) return@map Pair(it, ProfileEntry())
            return@map Pair(it, profiles[key])
        }

        var sortedHandlerProfile =
            handlerProfile.sortedWith(strategyComparators[id]!!).toMutableList()

        if (id == STRATEGY_RISING){
            val filteredLow = handlerProfile.filter{ cutoffComparator(it, RISING_CUTOFF_SECONDS) }
            val filteredHigh = handlerProfile.filterNot{ cutoffComparator(it, RISING_CUTOFF_SECONDS) }

            val sortedLow = filteredLow.sortedWith(strategyComparators[id]!!).toMutableList()
            val sortedHigh = filteredHigh.sortedWith(strategyComparators[id]!!).toMutableList()

            sortedHandlerProfile = (sortedLow + sortedHigh).toMutableList()
        }

        if (id == STRATEGY_HOT) {
            val filteredLow = handlerProfile.filter{ cutoffComparator(it, HOT_CUTOFF_SECONDS) }
            val filteredHigh = handlerProfile.filterNot{ cutoffComparator(it, HOT_CUTOFF_SECONDS) }

            val sortedLow = filteredLow.sortedWith(strategyComparators[id]!!).toMutableList()
            val sortedHigh = filteredHigh.sortedWith(strategyComparators[id]!!).toMutableList()

            sortedHandlerProfile = (sortedLow + sortedHigh).toMutableList()
        }

        return sortedHandlerProfile.map { it.first }.toMutableList()
    }


}
