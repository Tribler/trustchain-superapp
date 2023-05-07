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
    alphaDecay: Double,
    betaDecay: Double,
    graph: SimpleDirectedWeightedGraph<Node, NodeTrustEdge>,
    val heapEfficientImplementation: Boolean = true
): IncrementalRandomWalkedBasedRankingAlgo<SimpleDirectedWeightedGraph<Node, NodeTrustEdge>, Node, NodeTrustEdge>(maxWalkLength, repetitions, rootNode) {
    private val logger = KotlinLogging.logger {}
    private val iter = CustomRandomWalkVertexIterator(graph, rootNode, maxWalkLength.toLong(), alphaDecay, Random())
    private val betaDecayThreshold = 1.0 - betaDecay
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
        val betaDecays = if(heapEfficientImplementation) calculateBetaDecays(nodeCounts) else calculateBetaDecaysSpaceIntensive()
        val totalOccs = nodeCounts.values.sum()
        for((node, occ) in nodeCounts) {
            val betaDecay = 1.0 - (betaDecays[node] ?: 0.0)
            val betaDecayedScore = (occ.toDouble() / totalOccs) * betaDecay
            node.setPersonalizedPageRankScore(betaDecayedScore)
        }
    }

    private fun calculateBetaDecays(nodeCounts: Map<Node, Int>): Map<Node, Double> {
        val betaDecay = mutableMapOf<Node, Double>()
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
            val score = maxVisitsFromAnotherNode.toDouble() / totalVisits
            betaDecay[node] = if(score > betaDecayThreshold) score else 0.0
        }
        return betaDecay
    }

    private fun calculateBetaDecaysSpaceIntensive(): Map<Node, Double> {
        val betaDecay = mutableMapOf<Node, Double>()
        val totalVisitsToNode = mutableMapOf<Node, Int>()
        val visitToNodeThroughOtherNode = mutableMapOf<Node, MutableMap<Node, Int>>()
        for(walk in randomWalks) {
            val uniqueNodes = mutableSetOf<Node>()
            for(node in walk) {
                if(!uniqueNodes.contains(node)) {
                    totalVisitsToNode[node] = (totalVisitsToNode[node] ?: 0) + 1
                    for (visitedNode in uniqueNodes) {
                        val existingMap = visitToNodeThroughOtherNode[node] ?: mutableMapOf()
                        existingMap[visitedNode] = (existingMap[visitedNode] ?: 0) + 1
                        visitToNodeThroughOtherNode[node] = existingMap
                    }
                    uniqueNodes.add(node)
                }
            }
        }
        for(node in totalVisitsToNode.keys) {
            val maxVisitsFromAnotherNode = visitToNodeThroughOtherNode[node]?.filter { it.key != rootNode }?.values?.max() ?: 0
            val score = maxVisitsFromAnotherNode.toDouble() / totalVisitsToNode[node]!!
            betaDecay[node] = if(score > betaDecayThreshold) score else 0.0
        }
        return betaDecay
    }


}
