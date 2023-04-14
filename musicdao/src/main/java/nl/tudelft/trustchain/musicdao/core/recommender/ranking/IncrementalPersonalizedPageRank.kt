package nl.tudelft.trustchain.musicdao.core.recommender.ranking

import androidx.annotation.VisibleForTesting
import mu.KotlinLogging
import nl.tudelft.trustchain.musicdao.core.recommender.model.Node
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeTrustEdge
import nl.tudelft.trustchain.musicdao.core.recommender.ranking.iterator.CustomRandomWalkVertexIterator
import org.jgrapht.graph.SimpleDirectedWeightedGraph
import java.util.*

class IncrementalPersonalizedPageRank (
    maxWalkLength: Int,
    repetitions: Int,
    rootNode: Node,
    resetProbability: Float,
    graph: SimpleDirectedWeightedGraph<Node, NodeTrustEdge>
): IncrementalRandomWalkedBasedRankingAlgo<SimpleDirectedWeightedGraph<Node, NodeTrustEdge>, Node, NodeTrustEdge>(maxWalkLength, repetitions, rootNode) {
    private val logger = KotlinLogging.logger {}
    private val iter = CustomRandomWalkVertexIterator(graph, rootNode, maxWalkLength.toLong(), resetProbability, Random())
    lateinit var randomWalks: MutableList<MutableList<Node>>

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

    override fun initiateRandomWalks() {
        randomWalks = mutableListOf()
        for(walk in 0 until repetitions) {
            randomWalks.add(performNewRandomWalk())
        }
    }

    fun modifyEdges(sourceNodes: Set<Node>) {
        iter.modifyEdges(sourceNodes)
        for(i in 0 until randomWalks.size) {
            val walk = randomWalks[i]
            for(j in 0 until walk.size) {
                if(sourceNodes.contains(walk[j])) {
                    randomWalks[i] = walk.slice(0..j).toMutableList()
                    completeExistingRandomWalk(randomWalks[i])
                }
            }
        }
    }

    override fun calculateRankings() {
        val nodeCounts = randomWalks.flatten().groupingBy { it }.eachCount().filterKeys { it != rootNode }
        val totalOccs = nodeCounts.values.sum()
        for((node, occ) in nodeCounts) {
            node.setPersonalizedPageRankScore(occ.toDouble() / totalOccs)
        }
    }

}
