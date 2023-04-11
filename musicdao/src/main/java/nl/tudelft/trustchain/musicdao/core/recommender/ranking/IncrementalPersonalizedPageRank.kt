package nl.tudelft.trustchain.musicdao.core.recommender.ranking

import mu.KotlinLogging
import nl.tudelft.trustchain.musicdao.core.recommender.model.Node
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeTrustEdge
import nl.tudelft.trustchain.musicdao.core.recommender.ranking.iterator.CustomRandomWalkVertexIterator
import org.jgrapht.graph.SimpleDirectedWeightedGraph
import java.util.*

class IncrementalPersonalizedPageRank (
    private val maxWalkLength: Int,
    private val repetitions: Int,
    private val rootNode: Node,
    resetProbability: Float,
    graph: SimpleDirectedWeightedGraph<Node, NodeTrustEdge>
) {
    private val logger = KotlinLogging.logger {}
    private val iter = CustomRandomWalkVertexIterator(graph, rootNode, maxWalkLength.toLong(), resetProbability, Random())
    private val randomWalks: MutableList<MutableList<Node>> = mutableListOf()

    init {
        initiateRandomWalks()
    }

    private fun completeExistingRandomWalk(existingWalk: MutableList<Node>) {
        if(existingWalk.size == 0) {
            existingWalk.add(rootNode)
        }
        if(existingWalk.size >= maxWalkLength) {
            logger.info { "Random walk requested for already complete or overfull random walk" }
            return
        }
        iter.nextVertex = existingWalk.last()
        iter.hops = existingWalk.size.toLong() - 1
        iter.next()
        while(iter.hasNext()) {
            existingWalk.add(iter.next())
        }
    }

    private fun performNewRandomWalk(): MutableList<Node> {
        val randomWalk: MutableList<Node> = mutableListOf()
        randomWalk.add(rootNode)
        completeExistingRandomWalk(randomWalk)
        return randomWalk
    }

    fun initiateRandomWalks() {
        for(walk in 0 until repetitions) {
            randomWalks.add(performNewRandomWalk())
        }
    }

    fun modifyEdge(sourceNode: Node) {
        iter.modifyEdge(sourceNode)
        for(i in 0 until randomWalks.size) {
            val walk = randomWalks[i]
            for(j in 0 until walk.size) {
                if(walk[j] == sourceNode) {
                    randomWalks[i] = walk.slice(0..j).toMutableList()
                    completeExistingRandomWalk(randomWalks[i])
                }
            }
        }
    }

    fun calculatePersonalizedPageRank() {
        val nodeCounts = randomWalks.flatten().groupingBy { it }.eachCount().filterKeys { it != rootNode }
        val totalOccs = nodeCounts.values.sum()
        for((node, occ) in nodeCounts) {
            node.personalisedPageRank = (occ.toDouble() / totalOccs)
        }
    }

}
