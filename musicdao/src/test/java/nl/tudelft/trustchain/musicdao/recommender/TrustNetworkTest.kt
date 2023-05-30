package nl.tudelft.trustchain.musicdao.recommender

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import nl.tudelft.trustchain.musicdao.core.recommender.gossip.EdgeGossiper
import nl.tudelft.trustchain.musicdao.core.recommender.graph.*
import nl.tudelft.trustchain.musicdao.core.recommender.model.*
import nl.tudelft.trustchain.musicdao.core.recommender.networks.SerializedSubNetworks
import nl.tudelft.trustchain.musicdao.core.recommender.networks.SubNetworks
import nl.tudelft.trustchain.musicdao.core.recommender.networks.TrustNetwork
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
    private val logger = KotlinLogging.logger {}

    private fun setUp() {
        nodeToNodeNetwork = NodeToNodeNetwork()
        nodeToSongNetwork = NodeToSongNetwork()
        for(node in 0 until nNodes) {
            val nodeToAdd = Node(node.toString())
            nodeToNodeNetwork.addNode(nodeToAdd)
            nodeToSongNetwork.addNodeOrSong(nodeToAdd)
        }
        for(song in 0 until nSongs) {
            nodeToSongNetwork.addNodeOrSong(Recommendation(song.toString()))
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
                val randomNode = if(randomNum < node.getKey().toInt()) randomNum else randomNum + 1
                nodeToNodeNetwork.addEdge(node, allNodes[randomNode], NodeTrustEdge(rng.nextDouble(), Timestamp(rng.nextLong(minTimestamp, maxTimestamp))))
            }
        }
    }

    @Test
    fun canCreateATrustNetworkFromSubnetworks() {
        setUp()
        trustNetwork = TrustNetwork(SubNetworks(nodeToNodeNetwork, nodeToSongNetwork), 0.toString())
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
        val newSongRec = Recommendation("someHash")
        trustNetwork.addSongRec(newSongRec)
        edgeOneAdded = trustNetwork.addNodeToSongEdge(NodeRecEdge(NodeSongEdge(4.2), rootNode, newSongRec))
        edgeTwoAdded = trustNetwork.addNodeToSongEdge(NodeRecEdge(NodeSongEdge(5.2), newNode, newSongRec))
        Assert.assertTrue(edgeOneAdded)
        Assert.assertTrue(edgeTwoAdded)
        Assert.assertEquals(initialNodeToSongEdgeSize + 2, trustNetwork.getAllNodeToSongEdges().size)
    }

    @Test
    fun canSerializeTrustNetworkAndDeserializeFromIt() {
        setUp()
        trustNetwork = TrustNetwork(SubNetworks(nodeToNodeNetwork, nodeToSongNetwork), 0.toString())
        val newNode = Node(nNodes.toString())
        trustNetwork.addNode(newNode)
        trustNetwork.addNodeToNodeEdge(NodeTrustEdgeWithSourceAndTarget(NodeTrustEdge(4.2), rootNode, newNode))
        trustNetwork.addNodeToNodeEdge(NodeTrustEdgeWithSourceAndTarget(NodeTrustEdge(5.2), newNode, rootNode))
        val newSongRec = Recommendation("someHash")
        trustNetwork.addSongRec(newSongRec)
        trustNetwork.addNodeToSongEdge(NodeRecEdge(NodeSongEdge(4.2), rootNode, newSongRec))
        trustNetwork.addNodeToSongEdge(NodeRecEdge(NodeSongEdge(5.2), newNode, newSongRec))
        val serializedTrustNetwork = trustNetwork.serialize()
        val stringifiedTrustNetwork = Json.encodeToString(serializedTrustNetwork)
        val trustNetworkSubNetworks = Json.decodeFromString<SerializedSubNetworks>(stringifiedTrustNetwork)
        val newTrustNetwork = TrustNetwork(trustNetworkSubNetworks, rootNode.getKey())
        Assert.assertEquals(serializedTrustNetwork.nodeToNodeNetworkSerialized, trustNetworkSubNetworks.nodeToNodeNetworkSerialized)
        Assert.assertEquals(serializedTrustNetwork.nodeToSongNetworkSerialized, trustNetworkSubNetworks.nodeToSongNetworkSerialized)
        Assert.assertEquals(trustNetwork.nodeToNodeNetwork.graph, newTrustNetwork.nodeToNodeNetwork.graph)
        Assert.assertEquals(trustNetwork.nodeToSongNetwork.graph, newTrustNetwork.nodeToSongNetwork.graph)
    }

    @Test
    fun heapSpaceTest() {
        // Get current size of heap in bytes
        // Get current size of heap in bytes
        val heapSize = Runtime.getRuntime().totalMemory()
        // Get maximum size of heap in bytes. The heap cannot grow beyond this size.// Any attempt will result in an OutOfMemoryException.
        // Get maximum size of heap in bytes. The heap cannot grow beyond this size.// Any attempt will result in an OutOfMemoryException.
        val heapMaxSize = Runtime.getRuntime().maxMemory()
        // Get amount of free memory within the heap in bytes. This size will increase // after garbage collection and decrease as new objects are created.
        // Get amount of free memory within the heap in bytes. This size will increase // after garbage collection and decrease as new objects are created.
        val heapFreeSize = Runtime.getRuntime().freeMemory()

        Assert.assertEquals(1021021, heapMaxSize)
    }

}
