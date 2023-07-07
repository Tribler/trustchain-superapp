package nl.tudelft.trustchain.musicdao.core.recommender.ranking

import mu.KotlinLogging
import nl.tudelft.trustchain.musicdao.core.recommender.model.*
import nl.tudelft.trustchain.musicdao.core.recommender.ranking.iterator.CustomHybridRandomWalkWithTrustedRandomSurfer
import org.jgrapht.graph.DefaultUndirectedWeightedGraph
import org.jgrapht.graph.SimpleDirectedWeightedGraph
import java.util.*

class IncrementalHybridPersonalizedPageRankSalsaMeritRank(
    maxWalkLength: Int,
    repetitions: Int,
    rootNode: Node,
    alphaDecay: Double,
    private val betaDecay: Double,
    private val betaDecayThreshold: Double = 0.95,
    pageRankBalance: Double,
    graph: DefaultUndirectedWeightedGraph<NodeOrSong, NodeSongEdge>,
    nodeToNodeGraph: SimpleDirectedWeightedGraph<Node, NodeTrustEdge>
) : IncrementalRandomWalkedBasedRankingAlgo<DefaultUndirectedWeightedGraph<NodeOrSong, NodeSongEdge>, NodeOrSong, NodeSongEdge>(
    maxWalkLength,
    repetitions,
    rootNode
) {
    private val logger = KotlinLogging.logger {}
    private val iter =
        CustomHybridRandomWalkWithTrustedRandomSurfer(
            graph,
            nodeToNodeGraph,
            rootNode,
            maxWalkLength.toLong(),
            alphaDecay,
            pageRankBalance,
            Random(),
            graph.vertexSet().filterIsInstance<Node>()
                .toList()
        )
    lateinit var randomWalks: MutableList<MutableList<NodeOrSong>>

    init {
        initiateRandomWalks()
    }

    private fun performRandomWalk(existingWalk: MutableList<NodeOrSong>) {
        if (existingWalk.size >= maxWalkLength) {
            logger.info { "Random walk requested for already complete or overfull random walk" }
            return
        }
        iter.nextVertex = rootNode
        existingWalk.add(iter.next())
        while (iter.hasNext()) {
            existingWalk.add(iter.next())
        }
    }

    override fun calculateRankings() {
        val songsInWalks = randomWalks.flatten().filterIsInstance<Recommendation>()
        val songsToValue = mutableMapOf<Recommendation, Double>()
        val betaDecays = calculateBetaDecaysSpaceIntensive()
        for (song in songsInWalks) {
            songsToValue[song] = 0.0
        }
        for (walk in randomWalks) {
            var modifier = 1.0
            walk.forEachIndexed { index, nodeOrSong ->
                if (index % 2 == 0) {
                    modifier = 1.0
                } else {
                    songsToValue[nodeOrSong as Recommendation] = songsToValue[nodeOrSong]!! + modifier
                }
            }
        }
        val totalOccs = songsToValue.values.sum()
        for ((songRec, value) in songsToValue) {
            val decay = (1.0 - (betaDecays[songRec]?.let { it * betaDecay } ?: 0.0))
            songRec.rankingScore = (value / totalOccs) * decay
        }
    }

    fun modifyNodesOrSongs(changedNodes: Set<Node>, newNodes: List<Node>) {
        iter.modifyEdges(changedNodes)
        iter.modifyPersonalizedPageRanks(newNodes)
        initiateRandomWalks()
    }

    private fun performNewRandomWalk(): MutableList<NodeOrSong> {
        val randomWalk: MutableList<NodeOrSong> = mutableListOf()
        performRandomWalk(randomWalk)
        return randomWalk
    }

    override fun initiateRandomWalks() {
        randomWalks = mutableListOf()
        for (walk in 0 until repetitions) {
            randomWalks.add(performNewRandomWalk())
        }
    }

    private fun calculateBetaDecaysSpaceIntensive(): Map<Recommendation, Double> {
        val betaDecay = mutableMapOf<Recommendation, Double>()
        val totalVisitsToRec = mutableMapOf<Recommendation, Int>()
        val visitToNodeThroughOtherNode = mutableMapOf<Recommendation, MutableMap<NodeOrSong, Int>>()
        for (walk in randomWalks) {
            val uniqueNodesOrSong = mutableSetOf<NodeOrSong>()
            for (nodeOrRec in walk) {
                if (nodeOrRec is Recommendation) {
                    if (!uniqueNodesOrSong.contains(nodeOrRec)) {
                        totalVisitsToRec[nodeOrRec] = (totalVisitsToRec[nodeOrRec] ?: 0) + 1
                        for (visitedNode in uniqueNodesOrSong) {
                            val existingMap = visitToNodeThroughOtherNode[nodeOrRec] ?: mutableMapOf()
                            existingMap[visitedNode] = (existingMap[visitedNode] ?: 0) + 1
                            visitToNodeThroughOtherNode[nodeOrRec] = existingMap
                        }
                    }
                }
                uniqueNodesOrSong.add(nodeOrRec)
            }
        }
        for (node in totalVisitsToRec.keys) {
            val maxVisitsFromAnotherNode =
                visitToNodeThroughOtherNode[node]?.filter { it.key != rootNode }?.values?.maxOrNull() ?: 0
            val score = maxVisitsFromAnotherNode.toDouble() / totalVisitsToRec[node]!!
            betaDecay[node] = if (score > betaDecayThreshold) score else 0.0
        }
        return betaDecay
    }
}
