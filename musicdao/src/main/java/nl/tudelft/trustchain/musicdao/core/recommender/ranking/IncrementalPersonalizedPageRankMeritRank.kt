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
    private val betaDecay: Double,
    private val betaDecayThreshold: Double = 0.95,
    graph: SimpleDirectedWeightedGraph<Node, NodeTrustEdge>,
    val heapEfficientImplementation: Boolean = false
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
        val totalOccs = nodeCounts.values.sum()
        val betaDecays = if(heapEfficientImplementation) calculateBetaDecays(nodeCounts) else calculateBetaDecaysSpaceIntensive()
        for((node, occ) in nodeCounts) {
            val decay = (1.0 - (betaDecays[node]?.let { it * betaDecay } ?: 0.0))
            val betaDecayedScore = (occ.toDouble() / totalOccs) * decay
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
            val uniqueNodesList = mutableListOf<Node>()
            for(node in walk) {
                if (node != rootNode) {
                    totalVisitsToNode[node] = (totalVisitsToNode[node] ?: 0) + 1
                    if (!uniqueNodes.contains(node)) {
                        for (visitedNode in uniqueNodes) {
                            val existingMap = visitToNodeThroughOtherNode[node] ?: mutableMapOf()
                            existingMap[visitedNode] = (existingMap[visitedNode] ?: 0) + 1
                            visitToNodeThroughOtherNode[node] = existingMap
                        }
                        uniqueNodes.add(node)
                    } else {
                        var lastOccurence = 0
                        for (i in uniqueNodesList.size - 1 downTo 0) {
                            if (uniqueNodesList[i] == node) {
                                lastOccurence = i
                            }
                        }
                        for (visitedNode in uniqueNodesList.slice(lastOccurence until uniqueNodesList.size).toSet()) {
                            val existingMap = visitToNodeThroughOtherNode[node] ?: mutableMapOf()
                            existingMap[visitedNode] = (existingMap[visitedNode] ?: 0) + 1
                            visitToNodeThroughOtherNode[node] = existingMap
                        }
                    }
                    uniqueNodesList.add(node)
                }
            }
        }
        for(node in totalVisitsToNode.keys) {
            val maxVisitsFromAnotherNode = visitToNodeThroughOtherNode[node]?.filter { it.key != rootNode }?.values?.maxOrNull() ?: 0
            val score = maxVisitsFromAnotherNode.toDouble() / totalVisitsToNode[node]!!
            betaDecay[node] = if(score > betaDecayThreshold) 1.0 else 0.0
        }
        return betaDecay
    }

}
