package nl.tudelft.trustchain.musicdao.recommender

import nl.tudelft.trustchain.musicdao.core.recommender.graph.NodeToNodeNetwork
import nl.tudelft.trustchain.musicdao.core.recommender.model.NetworkRepresentation
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
    private val serializedGraphWithTwoNodesString = """{"creator":"JGraphT JSON Exporter","version":"1","nodes":[{"id":"1","ipv8":"randomNode","pr":0.3},{"id":"2","ipv8":"anotherNode","pr":0.0}],"edges":[]}"""
    private val compactNodeToNodeGraph = """c
c SOURCE: Generated using a Custom Graph Exporter
c
p nodeToNode 3 4
n 1 yetAnotherNode
n 2 randomNode
n 3 anotherNode
e 2 3 0.5 1585451228000
e 1 3 0.3 0
e 3 2 0.2 1
e 2 1 0.2 2
"""
    private val serializedNodeToNodeGraphWithSingleEdgeTwoNodes =
        """{"creator":"JGraphT JSON Exporter","version":"1","nodes":[{"id":"1","ipv8":"randomNode","pr":0.3},{"id":"2","ipv8":"anotherNode","pr":0.0}],"edges":[{"source":"1","target":"2","trust":0.5,"timestamp":1585451228000}]}"""
    private val serializedNodeToNodeGraphWithSingleNode =
        """{"creator":"JGraphT JSON Exporter","version":"1","nodes":[{"id":"1","ipv8":"someNode","pr":0.0}],"edges":[]}"""
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
        val allEdges = nodeToNodeNetwork.getAllNodeToNodeNetworkEdges()
        Assert.assertEquals(1, allEdges.size)
        Assert.assertEquals(someNodeEdge, allEdges.first())
        Assert.assertEquals(
            dummyValue.toDouble(),
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
        val allEdges = nodeToNodeNetwork.getAllNodeToNodeNetworkEdges()
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
    fun canSerializeAndDeserializeGraphWithSingleNode() {
        nodeToNodeNetwork = NodeToNodeNetwork()
        nodeToNodeNetwork.addNode(someNodeWithoutData)
        val serializedGraph = nodeToNodeNetwork.serialize()
        val deserializedGraph = NodeToNodeNetwork(serializedGraph, NetworkRepresentation.JSON).graph
        Assert.assertEquals(serializedNodeToNodeGraphWithSingleNode, serializedGraph)
        Assert.assertEquals(nodeToNodeNetwork.graph, deserializedGraph)
    }

    @Test
    fun canSerializeAndDeserializeCompactTrustGraph() {
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
        val serializedGraph = nodeToNodeNetwork.serializeCompact()
        Assert.assertEquals(compactNodeToNodeGraph, serializedGraph)
    }

    @Test
    fun canSerializeAndDeserializeGraphWithTwoNodesAsJSON() {
        nodeToNodeNetwork = NodeToNodeNetwork()
        nodeToNodeNetwork.addNode(randomNodeWithRandomPageRank)
        nodeToNodeNetwork.addNode(anotherNodeWithoutData)
        val serializedGraph = nodeToNodeNetwork.serialize()
        val deserializedGraph = NodeToNodeNetwork(serializedGraph, NetworkRepresentation.JSON).graph
        Assert.assertEquals(serializedGraphWithTwoNodesString, serializedGraph)
        Assert.assertEquals(nodeToNodeNetwork.graph, deserializedGraph)
    }

    @Test
    fun canSerializeAndDeserializeGraphWithDataAndEdgesAsJSON() {
        nodeToNodeNetwork = NodeToNodeNetwork()
        nodeToNodeNetwork.addNode(randomNodeWithRandomPageRank)
        nodeToNodeNetwork.addNode(anotherNodeWithoutData)
        nodeToNodeNetwork.addEdge(
            randomNodeWithRandomPageRank,
            anotherNodeWithoutData,
            someNodeEdge
        )
        val serializedGraph = nodeToNodeNetwork.serialize()
        val deserializedGraph = NodeToNodeNetwork(serializedGraph, NetworkRepresentation.JSON).graph
        Assert.assertEquals(serializedNodeToNodeGraphWithSingleEdgeTwoNodes, serializedGraph)
        Assert.assertEquals(nodeToNodeNetwork.graph, deserializedGraph)
    }

    @Test
    fun canCreateANetworkFromSerializedJSONString() {
        nodeToNodeNetwork = NodeToNodeNetwork()
        nodeToNodeNetwork.addNode(randomNodeWithRandomPageRank)
        nodeToNodeNetwork.addNode(anotherNodeWithoutData)
        val newNodeToNodeNetworkGraph = NodeToNodeNetwork(serializedGraphWithTwoNodesString, NetworkRepresentation.JSON).graph
        Assert.assertEquals(
            nodeToNodeNetwork.graph,
            newNodeToNodeNetworkGraph
        )
        Assert.assertEquals(
            randomNodeWithRandomPageRank.personalisedPageRank,
            newNodeToNodeNetworkGraph.vertexSet().first().personalisedPageRank,
            0.001
        )

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
        val newNodeToNodeNetwork = NodeToNodeNetwork(compactNodeToNodeGraph, NetworkRepresentation.COMPACT).graph
        Assert.assertEquals(
            nodeToNodeNetwork.graph,
            newNodeToNodeNetwork
        )
    }

}
