package nl.tudelft.trustchain.musicdao.recommender

import nl.tudelft.trustchain.musicdao.core.recommender.graph.NodeToSongNetwork
import nl.tudelft.trustchain.musicdao.core.recommender.model.Node
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeSongEdge
import nl.tudelft.trustchain.musicdao.core.recommender.model.Recommendation
import org.junit.Assert
import org.junit.Test
import java.sql.Timestamp

class NodeToSongTrustNetworkTest {
    private lateinit var network: NodeToSongNetwork
    private val someNode = Node("someNode")
    private val randomNodeWithRandomPageRank = Node("randomNode", 0.3)
    private val anotherNode = Node("anotherNode")
    private val someRecommendation = Recommendation("someTorrentHash", 0.5)
    private val anotherRecommendation = Recommendation("anotherTorrentHash", 0.8)
    private val dummyValue = 0.5
    private val anotherDummyValue = 0.3
    private val yetAnotherDummyValue = 0.4
    private val someNodeSongEdge = NodeSongEdge(
        dummyValue,
        Timestamp.valueOf("2020-03-29 05:07:08")
    )

    private val anotherNodeSongEdge = NodeSongEdge(
        anotherDummyValue,
        Timestamp(0)
    )

    private val yetAnotherNodeSongEdge = NodeSongEdge(
        yetAnotherDummyValue,
        Timestamp(1)
    )
    private val compactNodeToNodeGraph = """c
c SOURCE: Generated using a Custom Graph Exporter
c
p nodeToSong 4 3
n 1 someNode 0.0
n 2 randomNode 0.3
s 3 someTorrentHash 0.5
s 4 anotherTorrentHash 0.8
e 1 3 0.5 1585451228000
e 2 3 0.3 0
e 1 4 0.4 1
"""

    @Test
    fun canConstructAnEmptyNodeToSongNetwork() {
        network = NodeToSongNetwork()
        Assert.assertEquals(0, network.getAllSongs().size)
        Assert.assertEquals(0, network.getAllNodes().size)
        Assert.assertEquals(0, network.getAllEdges().size)
    }

    @Test
    fun canAddNodesToEmptyNodeToSongNetwork() {
        network = NodeToSongNetwork()
        network.addNodeOrSong(someNode)
        Assert.assertEquals(1, network.getAllNodes().size)
        Assert.assertEquals(0, network.getAllSongs().size)
    }

    @Test
    fun canAddSongsToEmptyNodeToSongNetwork() {
        network = NodeToSongNetwork()
        network.addNodeOrSong(someRecommendation)
        Assert.assertEquals(0, network.getAllNodes().size)
        Assert.assertEquals(1, network.getAllSongs().size)
    }

    @Test
    fun canAddEdgesBetweenNodesAndSongs() {
        network = NodeToSongNetwork()
        network.addNodeOrSong(someNode)
        network.addNodeOrSong(someRecommendation)
        Assert.assertEquals(1, network.getAllNodes().size)
        Assert.assertEquals(1, network.getAllSongs().size)
        val edgeAdded = network.addEdge(someNode, someRecommendation, someNodeSongEdge)
        Assert.assertTrue(edgeAdded)
        val edges = network.getAllEdges()
        Assert.assertEquals(someNodeSongEdge, edges.first())
        Assert.assertEquals(
            dummyValue,
            network.graph.getEdgeWeight(someNodeSongEdge),
            0.001
        )
    }

    @Test
    fun cannotAddEdgesBetweenNodesAndNodesOrSongsAndSongs() {
        network = NodeToSongNetwork()
        network.addNodeOrSong(someNode)
        network.addNodeOrSong(anotherNode)
        val edgeBetweenNodesAdded = network.addEdge(someNode, anotherNode, someNodeSongEdge)
        Assert.assertFalse(edgeBetweenNodesAdded)
        network.addNodeOrSong(someRecommendation)
        network.addNodeOrSong(anotherRecommendation)
        val edgeBetweenSongsAdded = network.addEdge(someRecommendation, anotherRecommendation, anotherNodeSongEdge)
        Assert.assertFalse(edgeBetweenSongsAdded)
    }

    @Test
    fun canRemoveEdgesFromNodeToSongNetwork() {
        network = NodeToSongNetwork()
        network.addNodeOrSong(someNode)
        network.addNodeOrSong(someRecommendation)
        val edgeAdded = network.addEdge(someNode, someRecommendation, someNodeSongEdge)
        Assert.assertTrue(edgeAdded)
        val edgeRemoved = network.removeEdge(someNodeSongEdge)
        Assert.assertTrue(edgeRemoved)
        val edges = network.getAllEdges()
        Assert.assertEquals(0, edges.size)
        Assert.assertEquals(0.0, network.graph.getEdgeWeight(someNodeSongEdge), 0.001)
    }

    @Test
    fun canSerializeAndDeserializeGraphWithCompactRepr() {
        network = NodeToSongNetwork()
        network.addNodeOrSong(someNode)
        network.addNodeOrSong(randomNodeWithRandomPageRank)
        network.addNodeOrSong(someRecommendation)
        network.addNodeOrSong(anotherRecommendation)

        network.addEdge(someNode, someRecommendation, someNodeSongEdge)
        network.addEdge(randomNodeWithRandomPageRank, someRecommendation, anotherNodeSongEdge)
        network.addEdge(someNode, anotherRecommendation, yetAnotherNodeSongEdge)

        val serializedOutput = network.serializeCompact()
        Assert.assertEquals(compactNodeToNodeGraph, serializedOutput)
        val newNodeToSongNetwork = NodeToSongNetwork(serializedOutput)
        Assert.assertEquals(
            network.graph,
            newNodeToSongNetwork.graph
        )
        Assert.assertEquals(
            newNodeToSongNetwork.getAllNodes().filter { it.getKey() == randomNodeWithRandomPageRank.getKey() }.first()
                .getPersonalizedPageRankScore(),
            randomNodeWithRandomPageRank.getPersonalizedPageRankScore(),
            0.001
        )
    }
}
