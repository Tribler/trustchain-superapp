package nl.tudelft.trustchain.musicdao.core.recommender.ranking

import mu.KotlinLogging
import nl.tudelft.trustchain.musicdao.core.recommender.ranking.iterator.CustomRandomWalkVertexIterator
import org.jgrapht.graph.SimpleDirectedWeightedGraph
import java.util.*

class IncrementalPersonalizedPageRank<V, E> (
    var storedRandomWalks: MutableList<MutableList<V>> = mutableListOf(),
    val maxWalkLength: Int,
    val repetitions: Int,
    val resetProbability: Float,
    val rootNode: V
) {
    private val logger = KotlinLogging.logger {}
    val randomWalks: MutableList<MutableList<V>> = mutableListWithCapacity(repetitions)

    init {
        randomWalks.addAll(storedRandomWalks)
    }
    private fun <T> mutableListWithCapacity(capacity: Int): MutableList<T> =
        ArrayList(capacity)
    fun completeExistingRandomWalk(graph: SimpleDirectedWeightedGraph<V, E>, existingWalk: MutableList<V>, seed: Long?) {
        if(existingWalk.size == 0) {
            existingWalk.add(rootNode)
        }
        if(existingWalk.size >= maxWalkLength) {
            logger.error { "Random walk requested for already complete or overfull random walk" }
            return
        }
        val iter = CustomRandomWalkVertexIterator<V, E>(graph, existingWalk.last(),
            (maxWalkLength - existingWalk.size).toLong(), true, resetProbability,
            if(seed == null) Random() else Random(seed))
        iter.next()
        while(iter.hasNext()) {
            existingWalk.add(iter.next())
        }
    }

    fun performNewRandomWalk(graph: SimpleDirectedWeightedGraph<V, E>, seed: Long?): MutableList<V> {
        val randomWalk: MutableList<V> = mutableListWithCapacity(maxWalkLength)
        randomWalk.add(rootNode)
        completeExistingRandomWalk(graph, randomWalk, seed)
        return randomWalk
    }

    fun initiateRandomWalks(graph: SimpleDirectedWeightedGraph<V, E>, seed: Long?) {
        for(walk in 0 until repetitions) {
            randomWalks[walk] = performNewRandomWalk(graph, seed)
        }
    }

}
