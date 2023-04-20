package nl.tudelft.trustchain.detoks

import android.util.Log
import nl.tudelft.trustchain.detoks.TorrentManager.TorrentHandler
import java.nio.channels.Selector
import kotlin.random.Random

class Strategy {

private val strategyComparators = mutableMapOf<Int, (Pair<TorrentHandler, ProfileEntry?>, Pair<TorrentHandler, ProfileEntry?>) -> Int>()

    var isSeeding = false

    var leechingStrategy = STRATEGY_RANDOM
    var seedingStrategy = STRATEGY_RANDOM

    var seedingBandwidthLimit = 0
    var storageLimit : Int = 0


    companion object {

        const val STRATEGY_RANDOM = 0
        const val STRATEGY_HOT = 1
        const val STRATEGY_RISING = 2
        const val STRATEGY_NEW = 3
        const val STRATEGY_TOP = 4
        const val STRATEGY_HOPCOUNT = 5
        const val STRATEGY_HIGHEST_WATCH_TIME = 6
        const val STRATEGY_LOWEST_WATCH_TIME = 7

        const val RISING_CUTOFF_SECONDS = 7200 * 1000 // 2 hour cutoff in milliseconds
        const val HOT_CUTOFF_SECONDS = 24 * 3600  * 1000//  1 day cutoff in milliseconds
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
    ) : Int = p1.second!!.likes compareTo p0.second!!.likes

    /**
     * Returns the torrent handlers based on when they were first uploaded
     */
    private fun newestFirstStrategy(
        p0: Pair<TorrentHandler, ProfileEntry?>,
        p1: Pair<TorrentHandler, ProfileEntry?>
    ) : Int = p1.second!!.uploadDate compareTo p0.second!!.uploadDate

    /**
     * Returns the torrent handlers based on hopcount
     */
    private fun hopCountStrategy(
        p0: Pair<TorrentHandler, ProfileEntry?>,
        p1: Pair<TorrentHandler, ProfileEntry?>
    ) : Int = p1.second!!.hopCount compareTo p0.second!!.hopCount

    /**
     * Determines if a torrent should be high in the list by checking the upload
     * time to the current time using a given cutoff time
     */
    private fun cutoffComparator(
        item: Pair<TorrentHandler, ProfileEntry?>,
        cutoff: Int
    ): Boolean {
        val currentTime = System.currentTimeMillis();
        val minimumDate = currentTime - cutoff

        return item.second!!.uploadDate >= minimumDate
    }

    private fun filteredSort(
        items: List<Pair<TorrentHandler, ProfileEntry?>>,
        strategyId: Int,
        filterCutoff: Int
    ): MutableList<Pair<TorrentHandler, ProfileEntry?>> {
            val filteredLow = items.filter{ cutoffComparator(it, filterCutoff) }
            val filteredHigh = items.filterNot{ cutoffComparator(it, filterCutoff) }

            val sortedLow = filteredLow.sortedWith(strategyComparators[strategyId]!!).toMutableList()
            val sortedHigh = filteredHigh.sortedWith(strategyComparators[strategyId]!!).toMutableList()

            return (sortedLow + sortedHigh).toMutableList()
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

        var sortedHandlerProfile: MutableList<Pair<TorrentHandler, ProfileEntry?>>

        if (id == STRATEGY_RISING){
            sortedHandlerProfile = filteredSort(handlerProfile, id, RISING_CUTOFF_SECONDS)
        } else if (id == STRATEGY_HOT) {
            sortedHandlerProfile = filteredSort(handlerProfile, id, HOT_CUTOFF_SECONDS)
        } else {
            sortedHandlerProfile =
                handlerProfile.sortedWith(strategyComparators[id]!!).toMutableList()
        }

        return sortedHandlerProfile.map { it.first }.toMutableList()
    }
    internal fun findLeechingIndex(
        handlers: MutableList<TorrentHandler>,
        profiles: HashMap<String, ProfileEntry>,
        newHandler: TorrentHandler,
        startIndex: Int
    ) : Int {
        if (leechingStrategy == 0 || (!strategyComparators.contains(leechingStrategy)))
            return Random.nextInt(startIndex, handlers.size)

        val handlerComparator =
            Comparator<TorrentHandler> { th0, th1 ->
                compareValuesBy(th0, th1, strategyComparators[leechingStrategy]!!) { handler ->
                    Pair(
                        handler,
                        profiles[handler.handle.infoHash().toString()]
                    )
                }
            }

        return handlers.binarySearch(newHandler,
            handlerComparator,
            startIndex,
            handlers.size
        )
    }
}
