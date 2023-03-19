package nl.tudelft.trustchain.detoks.gossiper

import kotlinx.coroutines.CoroutineScope
import nl.tudelft.ipv8.util.random

abstract class Gossiper {

    /**
     * Interval between gossips.
     */
    abstract val delay: Long

    /**
     * Amount of items to gossip.
     */
    abstract val blocks: Int

    /**
     * Number of peers to gossip with.
     */
    abstract val peers: Int


    /**
     * Instantiates the gossiping process.
     */
    abstract fun startGossip(coroutineScope: CoroutineScope)

    /**
     * The gossip routine.
     */
    abstract suspend fun gossip()

    /**
     * Returns a list of n random elements from collection.
     * If the collection is empty returns an empty list.
     * If n is larger than the collection size, returns the collection.
     */
    protected fun<T> pickRandomN(collection: List<T>, n: Int): List<T> {
        if (collection.isEmpty()) return listOf()
        if (collection.size < n) return collection

        return collection.random(n).toList()
    }
}
