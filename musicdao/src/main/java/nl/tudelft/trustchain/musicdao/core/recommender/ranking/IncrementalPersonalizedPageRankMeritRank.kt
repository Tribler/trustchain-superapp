package nl.tudelft.trustchain.musicdao.core.recommender.ranking

import mu.KotlinLogging
import nl.tudelft.trustchain.musicdao.core.recommender.model.Node
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeTrustEdge
import nl.tudelft.trustchain.musicdao.core.recommender.ranking.iterator.CustomRandomWalkVertexIterator
import org.jgrapht.graph.SimpleDirectedWeightedGraph
import java.util.*

class IncrementalPersonalizedPageRankMeritRank (
    maxWalkLength: Int,
    repetitions: Int,
    rootNode: Node,
    alphaDecay: Float,
    val betaDecayThreshold: Float,
    val betaDecay: Float,
    graph: SimpleDirectedWeightedGraph<Node, NodeTrustEdge>
): IncrementalRandomWalkedBasedRankingAlgo<SimpleDirectedWeightedGraph<Node, NodeTrustEdge>, Node, NodeTrustEdge>(maxWalkLength, repetitions, rootNode) {
    private val logger = KotlinLogging.logger {}
    private val iter = CustomRandomWalkVertexIterator(graph, rootNode, maxWalkLength.toLong(), alphaDecay, Random())
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
        val betaDecays = calculateBetaDecays()
        val nodeCounts = randomWalks.flatten().groupingBy { it }.eachCount().filterKeys { it != rootNode }
        val totalOccs = nodeCounts.values.sum()
        for((node, occ) in nodeCounts) {
            //effect on informativeness basd on decay using beta decay value
            val betaDecayedScore = (occ.toDouble() / totalOccs) * ( betaDecays[node]?.let { (1 - betaDecay).toDouble() } ?: 1.0)
            node.setPersonalizedPageRankScore(betaDecayedScore)
        }
    }

    private fun calculateBetaDecays(): Map<Node, Boolean> {
        val shouldBetaDecay = mutableMapOf<Node, Boolean>()
        val totalVisitsToNode = mutableMapOf<Node, Int>()
        val visitToNodeThroughOtherNode = mutableMapOf<Node, MutableMap<Node, Int>>()
        for(walk in randomWalks) {
            val uniqueNodes = mutableSetOf<Node>()
            for(node in walk) {
                totalVisitsToNode[node] = (totalVisitsToNode[node] ?: 0) + 1
                for(visitedNode in uniqueNodes) {
                    val existingMap = visitToNodeThroughOtherNode[node] ?: mutableMapOf()
                    existingMap[visitedNode] = (existingMap[visitedNode] ?: 0) + 1
                }
                uniqueNodes.add(node)
            }
        }
        for(node in totalVisitsToNode.keys) {
            val maxVisitsFromAnotherNode = visitToNodeThroughOtherNode[node]?.values?.sum() ?: 0
            val score = maxVisitsFromAnotherNode / totalVisitsToNode[node]!!
            shouldBetaDecay[node] = score > betaDecayThreshold
        }
        return shouldBetaDecay
    }

}
