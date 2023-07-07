package nl.tudelft.trustchain.musicdao.recommender

import nl.tudelft.trustchain.musicdao.core.recommender.graph.NodeToNodeNetwork
import nl.tudelft.trustchain.musicdao.core.recommender.model.Node
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeTrustEdge
import org.junit.Assert
import org.junit.Test
import java.sql.Timestamp

class NodeToNodeNetworkTest {
    private lateinit var nodeToNodeNetwork: NodeToNodeNetwork
    private val someNode = "someNode"
    private val randomNode = "randomNode"
    private val anotherNode = "anotherNode"
    private val someNodeWithoutData = Node(someNode)
    private val randomNodeWithRandomPageRank = Node(randomNode, 0.3)
    private val anotherNodeWithoutData = Node(anotherNode)

    private val dummyValue = 0.5
    private val anotherDummyValue = 0.3
    private val someNodeEdge = NodeTrustEdge(
        dummyValue,
        Timestamp.valueOf("2020-03-29 05:07:08")
    )

    private val anotherNodeEdge = NodeTrustEdge(
        anotherDummyValue,
        Timestamp(0)
    )

    @Test
    fun canConstructAnEmptyNodeToNodeNetwork() {
        nodeToNodeNetwork = NodeToNodeNetwork()
        Assert.assertEquals(0, nodeToNodeNetwork.getAllNodes().size)
    }

    @Test
    fun canAddNodesToEmptyNodeToNodeNetwork() {
        nodeToNodeNetwork = NodeToNodeNetwork()
        nodeToNodeNetwork.addNode(someNodeWithoutData)
        Assert.assertEquals(1, nodeToNodeNetwork.getAllNodes().size)
    }

    @Test
    fun canAddEdgesToEmptyNodeToNodeNetwork() {
        nodeToNodeNetwork = NodeToNodeNetwork()
        nodeToNodeNetwork.addNode(someNodeWithoutData)
        nodeToNodeNetwork.addNode(anotherNodeWithoutData)

        nodeToNodeNetwork.addEdge(someNodeWithoutData, anotherNodeWithoutData, someNodeEdge)
        val allEdges = nodeToNodeNetwork.getAllEdges()
        Assert.assertEquals(1, allEdges.size)
        Assert.assertEquals(someNodeEdge, allEdges.first())
        Assert.assertEquals(
            dummyValue.toDouble(),
            nodeToNodeNetwork.graph.getEdgeWeight(someNodeEdge),
            0.001
        )
    }

    @Test
    fun canRemoveEdgesFromNodeToNodeNetwork() {
        nodeToNodeNetwork = NodeToNodeNetwork()
        nodeToNodeNetwork.addNode(someNodeWithoutData)
        nodeToNodeNetwork.addNode(anotherNodeWithoutData)

        nodeToNodeNetwork.addEdge(someNodeWithoutData, anotherNodeWithoutData, someNodeEdge)
        var allEdges = nodeToNodeNetwork.getAllEdges()
        Assert.assertEquals(1, allEdges.size)
        nodeToNodeNetwork.removeEdge(someNodeEdge)
        allEdges = nodeToNodeNetwork.getAllEdges()
        Assert.assertEquals(0, allEdges.size)
        Assert.assertEquals(
            0.0,
            nodeToNodeNetwork.graph.getEdgeWeight(someNodeEdge),
            0.001
        )
    }

    @Test
    fun overwritesEdgeWhenANewEdgeIsAddedToTheSameNodes() {
        nodeToNodeNetwork = NodeToNodeNetwork()
        nodeToNodeNetwork.addNode(someNodeWithoutData)
        nodeToNodeNetwork.addNode(anotherNodeWithoutData)

        nodeToNodeNetwork.addEdge(someNodeWithoutData, anotherNodeWithoutData, someNodeEdge)
        nodeToNodeNetwork.addEdge(someNodeWithoutData, anotherNodeWithoutData, anotherNodeEdge)
        val allEdges = nodeToNodeNetwork.getAllEdges()
        Assert.assertEquals(1, allEdges.size)
        Assert.assertEquals(anotherNodeEdge, allEdges.first())
    }

    @Test
    fun addsNodeWithPageRank() {
        nodeToNodeNetwork = NodeToNodeNetwork()
        nodeToNodeNetwork.addNode(randomNodeWithRandomPageRank)
        val allNodes = nodeToNodeNetwork.getAllNodes()
        Assert.assertEquals(1, allNodes.size)
        Assert.assertEquals(randomNodeWithRandomPageRank, allNodes.first())
    }
}
