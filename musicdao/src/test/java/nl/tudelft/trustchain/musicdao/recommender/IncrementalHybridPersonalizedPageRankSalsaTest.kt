package nl.tudelft.trustchain.musicdao.recommender

import nl.tudelft.trustchain.musicdao.core.recommender.graph.NodeToSongNetwork
import nl.tudelft.trustchain.musicdao.core.recommender.model.Node
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeSongEdge
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeTrustEdge
import nl.tudelft.trustchain.musicdao.core.recommender.model.SongRecommendation
import nl.tudelft.trustchain.musicdao.core.recommender.ranking.IncrementalHybridPersonalizedPageRankSalsa
import nl.tudelft.trustchain.musicdao.core.recommender.ranking.IncrementalPersonalizedPageRank
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class IncrementalHybridPersonalizedPageRankSalsaTest {
    private var network: NodeToSongNetwork = NodeToSongNetwork()
    private lateinit var incrementalHybrid: IncrementalHybridPersonalizedPageRankSalsa
    private val seed = 42L
    private lateinit var rootNode: Node
    private val rng = Random(seed)
    private val nNodes = 5000
    private val nSongs = nNodes / 10
    private val nEdges = 10
    private val repetitions = 10000
    private val maxWalkLength = 10000

    @Before
    fun setUp() {
        for(node in 0 until nNodes) {
            network.addNodeOrSong(Node(node.toString()))
        }
        for(song in 0 until nSongs) {
            network.addNodeOrSong(SongRecommendation(song.toString()))
        }
        // Create 10 edges from each node to 10 random songs
        val allNodes = network.getAllNodes().toList()
        rootNode = allNodes[0]
        val allSongs = network.getAllSongs().toList()
        for(node in allNodes) {
            for(i in 0 until nEdges) {
                val randomNum = (0 until nSongs - 1).random(rng)
                network.addEdge(node, allSongs[randomNum], NodeSongEdge(rng.nextDouble()))
            }
        }
    }

    @Test
    fun canCaclulateScoreForSongs() {
        incrementalHybrid = IncrementalHybridPersonalizedPageRankSalsa(maxWalkLength, repetitions, rootNode, 0.01f, network.graph)
        incrementalHybrid.calculateRankings()
        Assert.assertEquals(1.0, network.getAllSongs().map { it.rankingScore }.sum(), 0.001)
    }

}
