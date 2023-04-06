package nl.tudelft.trustchain.musicdao.core.recommender.ranking

import mu.KotlinLogging
import nl.tudelft.trustchain.musicdao.core.recommender.model.Node
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeTrustEdge
import nl.tudelft.trustchain.musicdao.core.recommender.ranking.iterator.CustomRandomWalkVertexIterator
import org.jgrapht.graph.SimpleDirectedWeightedGraph
import java.util.*

class IncrementalPersonalizedPageRank (
    storedRandomWalks: MutableList<MutableList<Node>> = mutableListOf(),
    private val maxWalkLength: Int,
    private val repetitions: Int,
    private val resetProbability: Float,
    private val rootNode: Node
) {
    private val logger = KotlinLogging.logger {}
    val randomWalks: MutableList<MutableList<Node>> = mutableListWithCapacity(repetitions)

    init {
        randomWalks.addAll(storedRandomWalks)
    }
    private fun <T> mutableListWithCapacity(capacity: Int): MutableList<T> =
        ArrayList(capacity)
    fun completeExistingRandomWalk(graph: SimpleDirectedWeightedGraph<Node, NodeTrustEdge>, existingWalk: MutableList<Node>, seed: Long?) {
        if(existingWalk.size == 0) {
            existingWalk.add(rootNode)
        }
        if(existingWalk.size >= maxWalkLength) {
            logger.error { "Random walk requested for already complete or overfull random walk" }
            return
        }
        val iter = CustomRandomWalkVertexIterator(graph, existingWalk.last(),
            (maxWalkLength - existingWalk.size).toLong(), true, resetProbability,
            Random())
        iter.next()
        while(iter.hasNext()) {
            existingWalk.add(iter.next())
        }
    }

    fun performNewRandomWalk(graph: SimpleDirectedWeightedGraph<Node, NodeTrustEdge>, seed: Long?): MutableList<Node> {
        val randomWalk: MutableList<Node> = mutableListWithCapacity(maxWalkLength)
        randomWalk.add(rootNode)
        completeExistingRandomWalk(graph, randomWalk, seed)
        return randomWalk
    }

    fun initiateRandomWalks(graph: SimpleDirectedWeightedGraph<Node, NodeTrustEdge>, seed: Long?) {
        for(walk in 0 until repetitions) {
            randomWalks.add(performNewRandomWalk(graph, seed))
        }
    }

    fun calculatePersonalizedPageRank(graph: SimpleDirectedWeightedGraph<Node, NodeTrustEdge>) {
        val nodeCounts = randomWalks.flatten().groupingBy { it }.eachCount()
        val totalOccs = nodeCounts.values.sum()
        for((node, occ) in nodeCounts) {
            node.personalisedPageRank = (occ.toDouble() / totalOccs)
        }
    }

}
