package nl.tudelft.trustchain.musicdao.core.recommender.ranking

import mu.KotlinLogging
import nl.tudelft.trustchain.musicdao.core.recommender.model.*
import nl.tudelft.trustchain.musicdao.core.recommender.ranking.iterator.CustomHybridRandomWalkWithExplorationIterator
import org.jgrapht.graph.DefaultUndirectedWeightedGraph
import java.util.*

class IncrementalHybridPersonalizedPageRankSalsaMeritRank(
    maxWalkLength: Int,
    repetitions: Int,
    rootNode: Node,
    alphaDecay: Double,
    private val pageRankBalance: Double,
    graph: DefaultUndirectedWeightedGraph<NodeOrSong, NodeSongEdge>,
    val heapEfficientImplementation: Boolean = true
) : IncrementalRandomWalkedBasedRankingAlgo<DefaultUndirectedWeightedGraph<NodeOrSong, NodeSongEdge>, NodeOrSong, NodeSongEdge>(
    maxWalkLength,
    repetitions,
    rootNode
) {
    private val logger = KotlinLogging.logger {}
    private val iter =
        CustomHybridRandomWalkWithExplorationIterator(
            graph,
            rootNode,
            maxWalkLength.toLong(),
            alphaDecay,
            pageRankBalance,
            Random(),
            graph.vertexSet().filterIsInstance<Node>()
                .toList()
        )
    private val totalNodes = graph.vertexSet().filterIsInstance<Node>().size
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

//    override fun calculateRankings() {
//        val songCounts = randomWalks.flatten().groupingBy { it }.eachCount().filterKeys { it is SongRecommendation }
//        val betaDecays = if(heapEfficientImplementation) calculateBetaDecays(songCounts) else calculateBetaDecaysSpaceIntensive()
//        val totalOccs = songCounts.values.sum()
//        for ((songRec, occ) in songCounts) {
//            val betaDecay = 1.0 - (betaDecays[songRec] ?: 0.0)
//            songRec.rankingScore = (occ.toDouble() / totalOccs) * betaDecay
//        }
//    }

    override fun calculateRankings() {
        val songsInWalks = randomWalks.flatten().filterIsInstance<SongRecommendation>()
        val songsToValue = mutableMapOf<SongRecommendation, Double>()
        for(song in songsInWalks) {
            songsToValue[song] = 0.0
        }
        for(walk in randomWalks) {
            var modifier = 0.0
            walk.forEachIndexed { index, nodeOrSong ->
                if(index % 2 == 0) {
                    modifier = (1.0 - pageRankBalance) + (nodeOrSong.rankingScore * totalNodes * pageRankBalance)
                } else {
                    songsToValue[nodeOrSong as SongRecommendation] = songsToValue[nodeOrSong]!! + modifier
                }
            }
        }
        val totalOccs = songsToValue.values.sum()
        for ((songRec, value) in songsToValue) {
            songRec.rankingScore = (value / totalOccs)
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

//    private fun calculateBetaDecays(recCounts: Map<NodeOrSong, Int>): Map<SongRecommendation, Double> {
//        val betaDecay = mutableMapOf<SongRecommendation, Double>()
//        for (rec in recCounts.keys) {
//            val visitThroughAnotherNode = mutableMapOf<NodeOrSong, Int>()
//            var totalVisits = 0
//            for (walk in randomWalks) {
//                if (walk.contains(rec)) {
//                    val uniqueNodes = mutableSetOf<NodeOrSong>()
//                    totalVisits++
//                    for (visitNode in walk) {
//                        if (visitNode == rec) {
//                            break
//                        }
//                        if (!uniqueNodes.contains(visitNode)) {
//                            visitThroughAnotherNode[visitNode] =
//                                (visitThroughAnotherNode[visitNode] ?: 0) + 1
//                            uniqueNodes.add(visitNode)
//                        }
//                    }
//                }
//            }
//            visitThroughAnotherNode[rootNode] = 0
//            if(rec.identifier.contains("sybil")) {
//                print("bla")
//            }
//            val maxVisitsFromAnotherNode = visitThroughAnotherNode.values.max()
//            val score = maxVisitsFromAnotherNode.toDouble() / totalVisits
//            betaDecay[rec as SongRecommendation] = if(score > betaDecayThreshold) score else 0.0
//        }
//        return betaDecay
//    }
//
//    private fun calculateBetaDecaysSpaceIntensive(): Map<SongRecommendation, Double> {
//        val betaDecay = mutableMapOf<SongRecommendation, Double>()
//        val totalVisitsToRec = mutableMapOf<SongRecommendation, Int>()
//        val visitToNodeThroughOtherNode = mutableMapOf<SongRecommendation, MutableMap<NodeOrSong, Int>>()
//        for (walk in randomWalks) {
//            val uniqueNodesOrSong = mutableSetOf<NodeOrSong>()
//            for (nodeOrRec in walk) {
//                if (nodeOrRec is SongRecommendation) {
//                    if (!uniqueNodesOrSong.contains(nodeOrRec)) {
//                        totalVisitsToRec[nodeOrRec] = (totalVisitsToRec[nodeOrRec] ?: 0) + 1
//                        for (visitedNode in uniqueNodesOrSong) {
//                            val existingMap = visitToNodeThroughOtherNode[nodeOrRec] ?: mutableMapOf()
//                            existingMap[visitedNode] = (existingMap[visitedNode] ?: 0) + 1
//                            visitToNodeThroughOtherNode[nodeOrRec] = existingMap
//                        }
//                    }
//                }
//                uniqueNodesOrSong.add(nodeOrRec)
//            }
//        }
//        for (node in totalVisitsToRec.keys) {
//                val maxVisitsFromAnotherNode =
//                    visitToNodeThroughOtherNode[node]?.filter { it.key != rootNode }?.values?.maxOrNull() ?: 0
//                val score = maxVisitsFromAnotherNode.toDouble() / totalVisitsToRec[node]!!
//                betaDecay[node] = if(score > betaDecayThreshold) score else 0.0
//        }
//        return betaDecay
//    }
}
