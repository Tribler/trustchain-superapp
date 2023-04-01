package nl.tudelft.trustchain.musicdao.recommender

import nl.tudelft.trustchain.musicdao.core.recommender.graph.NodeToSongNetwork
import nl.tudelft.trustchain.musicdao.core.recommender.model.Node
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeSongEdge
import nl.tudelft.trustchain.musicdao.core.recommender.model.SongRecommendation
import org.junit.Assert
import org.junit.Test
import java.sql.Timestamp

class NodeToSongTrustNetworkTest {
    private lateinit var network: NodeToSongNetwork
    private val someNode = Node("someNode")
    private val randomNodeWithRandomPageRank = Node("randomNode", 0.3)
    private val anotherNode = Node("anotherNode")
    private val yetAnotherNode = Node("yetAnotherNode")
    private val someSongRecommendation = SongRecommendation("someTorrentHash")
    private val anotherSongRecommendation = SongRecommendation("anotherTorrentHash")
    private val yetAnotherSongRecommendation = SongRecommendation("yetAnotherTorrentHash")
    private val randomSongRecommendation = SongRecommendation("randomTorrentHash")
    private val dummyValue = 0.5
    private val anotherDummyValue = 0.3
    private val someNodeSongEdge = NodeSongEdge(
        dummyValue,
        Timestamp.valueOf("2020-03-29 05:07:08")
    )

    private val anotherNodeSongEdge = NodeSongEdge(
        anotherDummyValue,
        Timestamp(0)
    )

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
        network.addNodeOrSong(someSongRecommendation)
        Assert.assertEquals(0, network.getAllNodes().size)
        Assert.assertEquals(1, network.getAllSongs().size)
    }

    @Test
    fun canAddEdgesBetweenNodesAndSongs() {
        network = NodeToSongNetwork()
        network.addNodeOrSong(someNode)
        network.addNodeOrSong(someSongRecommendation)
        Assert.assertEquals(1, network.getAllNodes().size)
        Assert.assertEquals(1, network.getAllSongs().size)
        val edgeAdded = network.addEdge(someNode, someSongRecommendation, someNodeSongEdge)
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
        network.addNodeOrSong(someSongRecommendation)
        network.addNodeOrSong(anotherSongRecommendation)
        val edgeBetweenSongsAdded = network.addEdge(someSongRecommendation, anotherSongRecommendation, anotherNodeSongEdge)
        Assert.assertFalse(edgeBetweenSongsAdded)
    }

}
