package nl.tudelft.trustchain.musicdao.recommender

import nl.tudelft.trustchain.musicdao.core.recommender.gossip.EdgeGossiper
import nl.tudelft.trustchain.musicdao.core.recommender.graph.NodeToNodeNetwork
import nl.tudelft.trustchain.musicdao.core.recommender.graph.NodeToSongNetwork
import nl.tudelft.trustchain.musicdao.core.recommender.graph.TrustNetwork
import nl.tudelft.trustchain.musicdao.core.recommender.model.*
import org.junit.Assert
import org.junit.Test
import java.sql.Timestamp
import kotlin.random.Random

class TrustNetworkTest {
    private lateinit var nodeToSongNetwork: NodeToSongNetwork
    private lateinit var nodeToNodeNetwork: NodeToNodeNetwork
    private lateinit var trustNetwork: TrustNetwork
    private val seed = 42L
    private lateinit var rootNode: Node
    private val rng = Random(seed)
    private val nNodes = 5000
    private val nSongs = nNodes / 10
    private val nEdges = 10
    private val maxTimestamp = System.currentTimeMillis() + 10000
    private val minTimestamp = System.currentTimeMillis()
    private lateinit var edgeGossiper: EdgeGossiper

    private fun setUp() {
        nodeToNodeNetwork = NodeToNodeNetwork()
        nodeToSongNetwork = NodeToSongNetwork()
        for(node in 0 until nNodes) {
            val nodeToAdd = Node(node.toString())
            nodeToNodeNetwork.addNode(nodeToAdd)
            nodeToSongNetwork.addNodeOrSong(nodeToAdd)
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
    fun canCreateATrustNetworkFromUnderlyingNetworksAndAddEdgesAndNodesInIt() {
        setUp()
        trustNetwork = TrustNetwork(nodeToNodeNetwork, nodeToSongNetwork, 0.toString())
        // redundancy can lead to less than nNodes*nEdges edges i.e. if we add the same node trust edge twice
        val initialNodeToNodeEdgeSize = trustNetwork.getAllNodeToNodeEdges().size
        val initialNodeToSongEdgeSize = trustNetwork.getAllNodeToSongEdges().size
        val newNode = Node(nNodes.toString())
        trustNetwork.addNode(newNode)
        var edgeOneAdded = trustNetwork.addNodeToNodeEdge(NodeTrustEdgeWithSourceAndTarget(NodeTrustEdge(4.2), rootNode, newNode))
        var edgeTwoAdded = trustNetwork.addNodeToNodeEdge(NodeTrustEdgeWithSourceAndTarget(NodeTrustEdge(5.2), newNode, rootNode))
        Assert.assertTrue(edgeOneAdded)
        Assert.assertTrue(edgeTwoAdded)
        Assert.assertEquals(initialNodeToNodeEdgeSize + 2, trustNetwork.getAllNodeToNodeEdges().size)
        val newSongRec = SongRecommendation("someHash")
        trustNetwork.addSongRec(newSongRec)
        edgeOneAdded = trustNetwork.addNodeToSongEdge(NodeSongEdgeWithNodeAndSongRec(NodeSongEdge(4.2), rootNode, newSongRec))
        edgeTwoAdded = trustNetwork.addNodeToSongEdge(NodeSongEdgeWithNodeAndSongRec(NodeSongEdge(5.2), newNode, newSongRec))
        Assert.assertTrue(edgeOneAdded)
        Assert.assertTrue(edgeTwoAdded)
        Assert.assertEquals(initialNodeToSongEdgeSize + 2, trustNetwork.getAllNodeToSongEdges().size)
    }

}
