package nl.tudelft.trustchain.musicdao.core.recommender.ranking

import mu.KotlinLogging
import nl.tudelft.trustchain.musicdao.core.recommender.model.*
import nl.tudelft.trustchain.musicdao.core.recommender.ranking.iterator.CustomHybridRandomWalkIterator
import nl.tudelft.trustchain.musicdao.core.recommender.ranking.iterator.CustomRandomWalkVertexIterator
import org.jgrapht.graph.DefaultUndirectedWeightedGraph
import org.jgrapht.graph.SimpleDirectedWeightedGraph
import java.util.*

class IncrementalHybridPersonalizedPageRankSalsa (
    maxWalkLength: Int,
    repetitions: Int,
    rootNode: Node,
    resetProbability: Float,
    graph: DefaultUndirectedWeightedGraph<NodeOrSong, NodeSongEdge>
): IncrementalRandomWalkedBasedRankingAlgo<DefaultUndirectedWeightedGraph<NodeOrSong, NodeSongEdge>, NodeOrSong, NodeSongEdge>(maxWalkLength, repetitions, rootNode) {
    private val logger = KotlinLogging.logger {}
    private val iter = CustomHybridRandomWalkIterator(graph, rootNode, maxWalkLength.toLong(), resetProbability, Random())
    private lateinit var randomWalks: MutableList<MutableList<NodeOrSong>>

    init {
        initiateRandomWalks()
    }

    private fun completeExistingRandomWalk(existingWalk: MutableList<NodeOrSong>) {
        if(existingWalk.size == 0) {
            existingWalk.add(rootNode)
        }
        if(existingWalk.size >= maxWalkLength) {
            logger.info { "Random walk requested for already complete or overfull random walk" }
            return
        }
        iter.nextVertex = existingWalk.last()
        if(existingWalk.last() is SongRecommendation) {
            if(existingWalk.size < 2) {
                throw RuntimeException("Random Walk started with song recommendation")
            }
            val lastNode = existingWalk[existingWalk.size - 2]
            if(lastNode is Node) {
                iter.setLastNode(lastNode)
            } else {
                throw RuntimeException("Item in Random Walk before song recommendation isn't a node")
            }
        }
        iter.hops = existingWalk.size.toLong() - 1
        iter.next()
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

    private fun performNewRandomWalk(): MutableList<NodeOrSong> {
        val randomWalk: MutableList<NodeOrSong> = mutableListOf()
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
}
