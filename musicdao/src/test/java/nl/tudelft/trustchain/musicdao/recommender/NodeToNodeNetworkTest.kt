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
    private val yetAnotherNode = "yetAnotherNode"
    private val someNodeWithoutData = Node(someNode)
    private val randomNodeWithRandomPageRank = Node(randomNode, 0.3)
    private val anotherNodeWithoutData = Node(anotherNode)
    private val yetAnotherNodeWithoutData = Node(yetAnotherNode)
    private val compactNodeToNodeGraph = """c
c SOURCE: Generated using a Custom Graph Exporter
c
p nodeToNode 3 4
n 1 yetAnotherNode 0.0
n 2 randomNode 0.3
n 3 anotherNode 0.0
e 2 3 0.5 1585451228000
e 1 3 0.3 0
e 3 2 0.2 1
e 2 1 0.2 2
"""
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

    @Test
    fun canSerializeCompactTrustGraph() {
        nodeToNodeNetwork = NodeToNodeNetwork()
        nodeToNodeNetwork.addNode(yetAnotherNodeWithoutData)
        nodeToNodeNetwork.addNode(randomNodeWithRandomPageRank)
        nodeToNodeNetwork.addNode(anotherNodeWithoutData)
        nodeToNodeNetwork.addEdge(
            randomNodeWithRandomPageRank,
            anotherNodeWithoutData,
            someNodeEdge
        )
        nodeToNodeNetwork.addEdge(
            yetAnotherNodeWithoutData,
            anotherNodeWithoutData,
            anotherNodeEdge
        )
        nodeToNodeNetwork.addEdge(
            anotherNodeWithoutData,
            randomNodeWithRandomPageRank,
            NodeTrustEdge(0.2, Timestamp(1))
        )
        nodeToNodeNetwork.addEdge(
            randomNodeWithRandomPageRank,
            yetAnotherNodeWithoutData,
            NodeTrustEdge(0.2, Timestamp(2))
        )
        val serializedGraph = nodeToNodeNetwork.serialize()
        Assert.assertEquals(compactNodeToNodeGraph, serializedGraph)
    }


    @Test
    fun canCreateANetworkFromSerializedCompactString() {
        nodeToNodeNetwork = NodeToNodeNetwork()
        nodeToNodeNetwork.addNode(yetAnotherNodeWithoutData)
        nodeToNodeNetwork.addNode(randomNodeWithRandomPageRank)
        nodeToNodeNetwork.addNode(anotherNodeWithoutData)
        nodeToNodeNetwork.addEdge(
            randomNodeWithRandomPageRank,
            anotherNodeWithoutData,
            someNodeEdge
        )
        nodeToNodeNetwork.addEdge(
            yetAnotherNodeWithoutData,
            anotherNodeWithoutData,
            anotherNodeEdge
        )
        nodeToNodeNetwork.addEdge(
            anotherNodeWithoutData,
            randomNodeWithRandomPageRank,
            NodeTrustEdge(0.2, Timestamp(1))
        )
        nodeToNodeNetwork.addEdge(
            randomNodeWithRandomPageRank,
            yetAnotherNodeWithoutData,
            NodeTrustEdge(0.2, Timestamp(2))
        )
        val newNodeToNodeNetwork = NodeToNodeNetwork(compactNodeToNodeGraph)
        Assert.assertEquals(
            nodeToNodeNetwork.graph,
            newNodeToNodeNetwork.graph
        )
        Assert.assertEquals(
            newNodeToNodeNetwork.getAllNodes().filter { it.getKey() == randomNode }.first().getPersonalizedPageRankScore(),
            randomNodeWithRandomPageRank.getPersonalizedPageRankScore(),
            0.001
        )
    }

}
