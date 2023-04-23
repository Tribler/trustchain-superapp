package nl.tudelft.trustchain.musicdao.recommender

import nl.tudelft.trustchain.musicdao.core.recommender.gossip.EdgeGossiper
import nl.tudelft.trustchain.musicdao.core.recommender.graph.NodeToNodeNetwork
import nl.tudelft.trustchain.musicdao.core.recommender.graph.NodeToSongNetwork
import nl.tudelft.trustchain.musicdao.core.recommender.graph.TrustNetwork
import nl.tudelft.trustchain.musicdao.core.recommender.model.*
import nl.tudelft.trustchain.musicdao.core.recommender.ranking.IncrementalHybridPersonalizedPageRankSalsa
import org.junit.Before
import org.junit.Test
import java.sql.Timestamp
import kotlin.random.Random

class EdgeGossiperTest {
    private var nodeToSongNetwork: NodeToSongNetwork = NodeToSongNetwork()
    private var nodeToNodeNetwork: NodeToNodeNetwork = NodeToNodeNetwork()
    private lateinit var trustNetwork: TrustNetwork
    private lateinit var incrementalHybrid: IncrementalHybridPersonalizedPageRankSalsa
    private val seed = 42L
    private lateinit var rootNode: Node
    private val rng = Random(seed)
    private val nNodes = 5000
    private val nSongs = nNodes / 10
    private val nEdges = 10
    private val maxTimestamp = System.currentTimeMillis() + 10000
    private val minTimestamp = System.currentTimeMillis()
    private lateinit var edgeGossiper: EdgeGossiper

    @Before
    fun setUp() {
        for(node in 0 until nNodes) {
            nodeToSongNetwork.addNodeOrSong(Node(node.toString()))
        }
        for(song in 0 until nSongs) {
            nodeToSongNetwork.addNodeOrSong(SongRecommendation(song.toString()))
        }
        // Create 10 edges from each node to 10 random songs
        val allNodes = nodeToSongNetwork.getAllNodes().toList()
        rootNode = allNodes[0]
        val allSongs = nodeToSongNetwork.getAllSongs().toList()
        for(node in allNodes) {
            for(i in 0 until nEdges) {
                var randomNum = (0 until nSongs - 1).random(rng)
                nodeToSongNetwork.addEdge(node, allSongs[randomNum], NodeSongEdge(rng.nextDouble(), Timestamp(rng.nextLong(minTimestamp, maxTimestamp))))
                randomNum = (0 until nNodes - 1).random(rng)
                val randomNode = if(randomNum < node.getIpv8().toInt()) randomNum else randomNum + 1
                nodeToNodeNetwork.addEdge(node, allNodes[randomNode], NodeTrustEdge(rng.nextDouble(), Timestamp(rng.nextLong(minTimestamp, maxTimestamp))))
            }
        }
    }

    @Test
    fun canInitializeDeltasAndWeightsForEdgeGossiping() {
//        edgeGossiper = EdgeGossiper()
    }




}
