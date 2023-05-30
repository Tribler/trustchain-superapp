package nl.tudelft.trustchain.musicdao.recommender

import nl.tudelft.trustchain.musicdao.core.recommender.gossip.EdgeGossiper
import nl.tudelft.trustchain.musicdao.core.recommender.graph.NodeToNodeNetwork
import nl.tudelft.trustchain.musicdao.core.recommender.graph.NodeToSongNetwork
import nl.tudelft.trustchain.musicdao.core.recommender.networks.SubNetworks
import nl.tudelft.trustchain.musicdao.core.recommender.networks.TrustNetwork
import nl.tudelft.trustchain.musicdao.core.recommender.model.*
import nl.tudelft.trustchain.musicdao.core.recommender.ranking.IncrementalHybridPersonalizedPageRankSalsa
import org.junit.Assert
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
    private val doubleDelta = 0.0000001
    private val maxTimestamp = System.currentTimeMillis() + 10000
    private val minTimestamp = System.currentTimeMillis()
    private lateinit var edgeGossiper: EdgeGossiper

    @Before
    fun setUp() {
        for (node in 0 until nNodes) {
            val nodeToAdd = Node(node.toString())
            nodeToNodeNetwork.addNode(nodeToAdd)
            nodeToSongNetwork.addNodeOrSong(nodeToAdd)
        }
        for (song in 0 until nSongs) {
            nodeToSongNetwork.addNodeOrSong(Recommendation(song.toString()))
        }
        // Create 10 edges from each node to 10 random songs
        val allNodes = nodeToSongNetwork.getAllNodes().toList()
        rootNode = allNodes[0]
        val allSongs = nodeToSongNetwork.getAllSongs().toList()
        for (node in allNodes) {
            for (i in 0 until nEdges) {
                var randomNum = (0 until nSongs - 1).random(rng)
                nodeToSongNetwork.addEdge(
                    node,
                    allSongs[randomNum],
                    NodeSongEdge(rng.nextDouble(), Timestamp(rng.nextLong(minTimestamp, maxTimestamp)))
                )
                randomNum = (0 until nNodes - 1).random(rng)
                val randomNode = if (randomNum < node.getKey().toInt()) randomNum else randomNum + 1
                nodeToNodeNetwork.addEdge(
                    node,
                    allNodes[randomNode],
                    NodeTrustEdge(rng.nextDouble(), Timestamp(rng.nextLong(minTimestamp, maxTimestamp)))
                )
            }
        }
    }

    @Test
    fun canInitializeDeltasForEdgeGossiping() {
        trustNetwork = TrustNetwork(SubNetworks(nodeToNodeNetwork, nodeToSongNetwork), rootNode.getKey())
        edgeGossiper = EdgeGossiper(RecommenderCommunityMock("someServiceId"), false, trustNetwork)
        val nodeToNodeDeltas = edgeGossiper.nodeToNodeEdgeDeltas
        val nodeToSongDeltas = edgeGossiper.nodeToSongEdgeDeltas

        val sortedNtNEdgesInWindow = nodeToNodeNetwork.getAllEdges().sortedBy { it.timestamp }.takeLast(EdgeGossiper.TIME_WINDOW)
        val oldestNodeToNodeEdge = sortedNtNEdgesInWindow.first()
        val newestNodeToNodeEdge = sortedNtNEdgesInWindow.last()
        val deltaOldestAndNewestNtNEdge = (newestNodeToNodeEdge.timestamp.time - oldestNodeToNodeEdge.timestamp.time).toInt()
        Assert.assertEquals(deltaOldestAndNewestNtNEdge, nodeToNodeDeltas.max())
        Assert.assertEquals(0, nodeToNodeDeltas.min())

        val sortedNtSEdgesInWindow = nodeToSongNetwork.getAllEdges().sortedBy { it.timestamp }.takeLast(EdgeGossiper.TIME_WINDOW)
        val oldestNodeToSongEdge = sortedNtSEdgesInWindow.first()
        val newestNodeToSongEdge = sortedNtSEdgesInWindow.last()
        val deltaOldestAndNewestNtSEdge = (newestNodeToSongEdge.timestamp.time - oldestNodeToSongEdge.timestamp.time).toInt()
        Assert.assertEquals(deltaOldestAndNewestNtSEdge, nodeToSongDeltas.max())
        Assert.assertEquals(0, nodeToSongDeltas.min())

    }

    @Test
    fun canInitializeWeightsBasedOnDeltaValues() {
        trustNetwork = TrustNetwork(SubNetworks(nodeToNodeNetwork, nodeToSongNetwork), rootNode.getKey())
        edgeGossiper = EdgeGossiper(RecommenderCommunityMock("someServiceId"), false, trustNetwork)
        val nodeToNodeDeltas = edgeGossiper.nodeToNodeEdgeDeltas
        val nodeToSongDeltas = edgeGossiper.nodeToSongEdgeDeltas
        val nodeToNodeWeights = edgeGossiper.nodeToNodeEdgeWeights
        val nodeToSongWeights = edgeGossiper.nodeToSongEdgeWeights

        val sortedNtNEdgesInWindow = nodeToNodeNetwork.getAllEdges().sortedBy { it.timestamp }.takeLast(EdgeGossiper.TIME_WINDOW)
        val oldestNodeToNodeEdge = sortedNtNEdgesInWindow.minBy { it.timestamp }
        val newestNodeToNodeEdge = sortedNtNEdgesInWindow.maxBy { it.timestamp }
        val sumNtNDeltas = nodeToNodeDeltas.sum()
        val expectedWeightOldestNtNEdge = 0.0
        val expectedWeightNewestNtNEdge = ((newestNodeToNodeEdge.timestamp.time - oldestNodeToNodeEdge.timestamp.time).toDouble() / sumNtNDeltas)
        Assert.assertEquals(expectedWeightOldestNtNEdge, nodeToNodeWeights.first(), doubleDelta)
        Assert.assertEquals(expectedWeightNewestNtNEdge, nodeToNodeWeights.last(), doubleDelta)

        val sortedNtSEdgesInWindow = nodeToSongNetwork.getAllEdges().sortedBy { it.timestamp }.takeLast(EdgeGossiper.TIME_WINDOW)
        val oldestNodeToSongEdge = sortedNtSEdgesInWindow.first()
        val newestNodeToSongEdge = sortedNtSEdgesInWindow.last()
        val sumNtSDeltas = nodeToSongDeltas.sum()
        val expectedWeightOldestNtSEdge = 0.0
        val expectedWeightNewestNtSEdge = ((newestNodeToSongEdge.timestamp.time - oldestNodeToSongEdge.timestamp.time).toDouble() / sumNtSDeltas)
        Assert.assertEquals(expectedWeightOldestNtSEdge, nodeToSongWeights.first(), doubleDelta)
        Assert.assertEquals(expectedWeightNewestNtSEdge, nodeToSongWeights.last(), doubleDelta)

        Assert.assertEquals(1.0, nodeToNodeWeights.sum(), doubleDelta)
        Assert.assertEquals(1.0, nodeToSongWeights.sum(), doubleDelta)
    }

    @Test
    fun usesTimeWindowToConstructDeltasAndWeights() {
        val someTimeWindow = Random.nextInt(1, EdgeGossiper.TIME_WINDOW)
        trustNetwork = TrustNetwork(SubNetworks(nodeToNodeNetwork, nodeToSongNetwork), rootNode.getKey())
        edgeGossiper = EdgeGossiper(RecommenderCommunityMock("someServiceId"), false, trustNetwork, someTimeWindow)
        val nodeToNodeDeltas = edgeGossiper.nodeToNodeEdgeDeltas
        val nodeToSongDeltas = edgeGossiper.nodeToSongEdgeDeltas
        val nodeToNodeWeights = edgeGossiper.nodeToNodeEdgeWeights
        val nodeToSongWeights = edgeGossiper.nodeToSongEdgeWeights

        Assert.assertEquals(someTimeWindow, nodeToNodeDeltas.size)
        Assert.assertEquals(someTimeWindow, nodeToSongDeltas.size)
        Assert.assertEquals(someTimeWindow, nodeToNodeWeights.size)
        Assert.assertEquals(someTimeWindow, nodeToSongWeights.size)
    }
}
