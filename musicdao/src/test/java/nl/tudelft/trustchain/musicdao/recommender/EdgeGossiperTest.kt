package nl.tudelft.trustchain.musicdao.recommender

import io.mockk.mockk
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.trustchain.musicdao.core.ipv8.TrustedRecommenderCommunity
import nl.tudelft.trustchain.musicdao.core.recommender.gossip.EdgeGossiperService
import nl.tudelft.trustchain.musicdao.core.recommender.graph.NodeToNodeNetwork
import nl.tudelft.trustchain.musicdao.core.recommender.graph.NodeToSongNetwork
import nl.tudelft.trustchain.musicdao.core.recommender.networks.SubNetworks
import nl.tudelft.trustchain.musicdao.core.recommender.networks.TrustNetwork
import nl.tudelft.trustchain.musicdao.core.recommender.model.*
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.sql.Timestamp
import kotlin.random.Random

class EdgeGossiperTest {
    private var nodeToSongNetwork: NodeToSongNetwork = NodeToSongNetwork()
    private var nodeToNodeNetwork: NodeToNodeNetwork = NodeToNodeNetwork()
    private lateinit var trustNetwork: TrustNetwork
    private val communityMock = mockk<TrustedRecommenderCommunity>(relaxed = true)
    private val seed = 42L
    private lateinit var rootNode: Node
    private val rng = Random(seed)
    private val nNodes = 5000
    private val nSongs = nNodes / 10
    private val nEdges = 10
    private val doubleDelta = 0.0000001
    private val maxTimestamp = System.currentTimeMillis() + 10000
    private val minTimestamp = System.currentTimeMillis()
    private lateinit var edgeGossiper: EdgeGossiperService

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
        edgeGossiper = EdgeGossiperService(communityMock)
        edgeGossiper.updateDeltasAndWeights(trustNetwork.getAllNodeToNodeEdges(), trustNetwork.getAllNodeToSongEdges())
        val nodeToNodeDeltas = edgeGossiper.nodeToNodeEdgeDeltas
        val nodeToSongDeltas = edgeGossiper.nodeToSongEdgeDeltas

        val sortedNtNEdgesInWindow = nodeToNodeNetwork.getAllEdges().sortedBy { it.timestamp }.takeLast(10000)
        val oldestNodeToNodeEdge = sortedNtNEdgesInWindow.first()
        val newestNodeToNodeEdge = sortedNtNEdgesInWindow.last()
        val deltaOldestAndNewestNtNEdge =
            (newestNodeToNodeEdge.timestamp.time - oldestNodeToNodeEdge.timestamp.time).toInt()
        Assert.assertEquals(deltaOldestAndNewestNtNEdge, nodeToNodeDeltas.max() - (1000 * nodeToNodeDeltas.size))
        Assert.assertEquals(0, nodeToNodeDeltas.min() - (1000 * nodeToNodeDeltas.size))

        val sortedNtSEdgesInWindow = nodeToSongNetwork.getAllEdges().sortedBy { it.timestamp }.takeLast(10000)
        val oldestNodeToSongEdge = sortedNtSEdgesInWindow.first()
        val newestNodeToSongEdge = sortedNtSEdgesInWindow.last()
        val deltaOldestAndNewestNtSEdge =
            (newestNodeToSongEdge.timestamp.time - oldestNodeToSongEdge.timestamp.time).toInt()
        Assert.assertEquals(deltaOldestAndNewestNtSEdge, nodeToSongDeltas.max() - (1000 * nodeToNodeDeltas.size))
        Assert.assertEquals(0, nodeToSongDeltas.min() - (1000 * nodeToNodeDeltas.size))
    }

    @Test
    fun usesTimeWindowToConstructDeltasAndWeights() {
        val timeWindow = EdgeGossiperService.TIME_WINDOW
        trustNetwork = TrustNetwork(SubNetworks(nodeToNodeNetwork, nodeToSongNetwork), rootNode.getKey())
        edgeGossiper = EdgeGossiperService(communityMock)
        edgeGossiper.updateDeltasAndWeights(trustNetwork.getAllNodeToNodeEdges(), trustNetwork.getAllNodeToSongEdges())
        val nodeToNodeDeltas = edgeGossiper.nodeToNodeEdgeDeltas
        val nodeToSongDeltas = edgeGossiper.nodeToSongEdgeDeltas
        val nodeToNodeWeights = edgeGossiper.nodeToNodeEdgeWeights
        val nodeToSongWeights = edgeGossiper.nodeToSongEdgeWeights

        Assert.assertEquals(timeWindow, nodeToNodeDeltas.size)
        Assert.assertEquals(timeWindow, nodeToSongDeltas.size)
        Assert.assertEquals(timeWindow, nodeToNodeWeights.size)
        Assert.assertEquals(timeWindow, nodeToSongWeights.size)
    }
}
