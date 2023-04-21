package nl.tudelft.trustchain.musicdao.core.recommender.ranking

import mu.KotlinLogging
import nl.tudelft.trustchain.musicdao.core.recommender.model.*
import nl.tudelft.trustchain.musicdao.core.recommender.ranking.iterator.CustomHybridRandomWalkRandomNodeResetIterator
import org.jgrapht.graph.DefaultUndirectedWeightedGraph
import java.util.*

class IncrementalHybridPersonalizedPageRankSalsa (
    maxWalkLength: Int,
    repetitions: Int,
    rootNode: Node,
    resetProbability: Float,
    graph: DefaultUndirectedWeightedGraph<NodeOrSong, NodeSongEdge>
): IncrementalRandomWalkedBasedRankingAlgo<DefaultUndirectedWeightedGraph<NodeOrSong, NodeSongEdge>, NodeOrSong, NodeSongEdge>(maxWalkLength, repetitions, rootNode) {
    private val logger = KotlinLogging.logger {}
    private val iter = CustomHybridRandomWalkRandomNodeResetIterator(graph, rootNode, maxWalkLength.toLong(), resetProbability, 0.5f, Random(), graph.vertexSet().filterIsInstance<Node>()
        .toList() )
    private lateinit var randomWalks: MutableList<MutableList<NodeOrSong>>

    init {
        initiateRandomWalks()
    }

    private fun performRandomWalk(existingWalk: MutableList<NodeOrSong>) {
        if(existingWalk.size >= maxWalkLength) {
            logger.info { "Random walk requested for already complete or overfull random walk" }
            return
        }
        iter.nextVertex = rootNode
        existingWalk.add(iter.next())
        while(iter.hasNext()) {
            existingWalk.add(iter.next())
        }
    }
    override fun calculateRankings() {
        val songCounts = randomWalks.flatten().groupingBy { it }.eachCount().filterKeys { it is SongRecommendation }
        val totalOccs = songCounts.values.sum()
        for((song, occ) in songCounts) {
            song.rankingScore = occ.toDouble() / totalOccs
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
        for(walk in 0 until repetitions) {
            randomWalks.add(performNewRandomWalk())
        }
    }
}
