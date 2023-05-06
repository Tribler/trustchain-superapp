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
        val nodeCounts = randomWalks.flatten().groupingBy { it }.eachCount().filterKeys { it != rootNode }
        val betaDecays = calculateBetaDecays(nodeCounts)
        val test = betaDecays[Node("sybil1")]
        val totalOccs = nodeCounts.values.sum()
        for((node, occ) in nodeCounts) {
            //effect on informativeness basd on decay using beta decay value
            val betaDecay = if(betaDecays[node]!!) (1 - betaDecay).toDouble() else 1.0
            val betaDecayedScore = (occ.toDouble() / totalOccs) * betaDecay
            node.setPersonalizedPageRankScore(betaDecayedScore)
        }
    }

    private fun calculateBetaDecays(nodeCounts: Map<Node, Int>): Map<Node, Boolean> {
        val shouldBetaDecay = mutableMapOf<Node, Boolean>()
        for(node in nodeCounts.keys) {
            val visitThroughAnotherNode = mutableMapOf<Node, Int>()
            var totalVisits = 0
            for(walk in randomWalks) {
                if(walk.contains(node)) {
                    val uniqueNodes = mutableSetOf<Node>()
                    totalVisits++
                    for(visitNode in walk) {
                        if (visitNode != node) {
                            if(!uniqueNodes.contains(visitNode)) {
                                visitThroughAnotherNode[visitNode] =
                                    visitThroughAnotherNode[visitNode]?.let { it + 1 } ?: 1
                                uniqueNodes.add(visitNode)
                            }
                        } else {
                            break
                        }
                    }
                }
            }
            visitThroughAnotherNode[rootNode] = 0
            val maxVisitsFromAnotherNode = visitThroughAnotherNode.values.max()
            val score = maxVisitsFromAnotherNode / totalVisits
            shouldBetaDecay[node] = score > betaDecayThreshold
        }
        return shouldBetaDecay
    }

}
