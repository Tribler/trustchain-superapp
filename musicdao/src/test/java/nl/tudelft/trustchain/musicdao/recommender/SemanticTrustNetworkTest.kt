package nl.tudelft.trustchain.musicdao.recommender

import nl.tudelft.trustchain.musicdao.core.recommender.graph.TrustNetwork
import nl.tudelft.trustchain.musicdao.core.recommender.model.Node
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeTrustEdge
import org.junit.Assert
import org.junit.Test
import java.sql.Timestamp

class SemanticTrustNetworkTest {
    private lateinit var semanticTrustNetwork: TrustNetwork
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
    fun canConstructAnEmptySemanticTrustNetwork() {
        semanticTrustNetwork = TrustNetwork()
        Assert.assertEquals(0, semanticTrustNetwork.getAllNodes().size)
    }

    @Test
    fun canAddNodesToEmptySemanticTrustNetwork() {
        semanticTrustNetwork = TrustNetwork()
        semanticTrustNetwork.addNodeToNodeNetworkNode(someNodeWithoutData)
        Assert.assertEquals(1, semanticTrustNetwork.getAllNodes().size)
    }

    @Test
    fun canAddEdgesToEmptySemanticTrustNetwork() {
        semanticTrustNetwork = TrustNetwork()
        semanticTrustNetwork.addNodeToNodeNetworkNode(someNodeWithoutData)
        semanticTrustNetwork.addNodeToNodeNetworkNode(anotherNodeWithoutData)

        semanticTrustNetwork.addNodeToNodeNetworkEdge(someNodeWithoutData, anotherNodeWithoutData, someNodeEdge)
        val allEdges = semanticTrustNetwork.getAllNodeToNodeNetworkEdges()
        Assert.assertEquals(1, allEdges.size)
        Assert.assertEquals(someNodeEdge, allEdges.first())
        Assert.assertEquals(
            dummyValue.toDouble(),
            semanticTrustNetwork.nodeToNodeNetwork.getEdgeWeight(someNodeEdge),
            0.001
        )
    }

    @Test
    fun overwritesEdgeWhenANewEdgeIsAddedToTheSameNodes() {
        semanticTrustNetwork = TrustNetwork()
        semanticTrustNetwork.addNodeToNodeNetworkNode(someNodeWithoutData)
        semanticTrustNetwork.addNodeToNodeNetworkNode(anotherNodeWithoutData)

        semanticTrustNetwork.addNodeToNodeNetworkEdge(someNodeWithoutData, anotherNodeWithoutData, someNodeEdge)
        semanticTrustNetwork.addNodeToNodeNetworkEdge(someNodeWithoutData, anotherNodeWithoutData, anotherNodeEdge)
        val allEdges = semanticTrustNetwork.getAllNodeToNodeNetworkEdges()
        Assert.assertEquals(1, allEdges.size)
        Assert.assertEquals(anotherNodeEdge, allEdges.first())
    }

    @Test
    fun addsNodeWithPageRank() {
        semanticTrustNetwork = TrustNetwork()
        semanticTrustNetwork.addNodeToNodeNetworkNode(randomNodeWithRandomPageRank)
        val allNodes = semanticTrustNetwork.getAllNodes()
        Assert.assertEquals(1, allNodes.size)
        Assert.assertEquals(randomNodeWithRandomPageRank, allNodes.first())
    }

    @Test
    fun canSerializeAndDeserializeGraphWithSingleNode() {
        semanticTrustNetwork = TrustNetwork()
        semanticTrustNetwork.addNodeToNodeNetworkNode(someNodeWithoutData)
        val serializedGraph = semanticTrustNetwork.serialize()
        val deserializedGraph = TrustNetwork.deserializeNodeToNodeJson(serializedGraph)
        Assert.assertEquals(serializedNodeToNodeGraphWithSingleNode, serializedGraph)
        Assert.assertEquals(semanticTrustNetwork.nodeToNodeNetwork, deserializedGraph)
    }

    @Test
    fun canSerializeAndDeserializeCompactTrustGraph() {
        semanticTrustNetwork = TrustNetwork()
        semanticTrustNetwork.addNodeToNodeNetworkNode(yetAnotherNodeWithoutData)
        semanticTrustNetwork.addNodeToNodeNetworkNode(randomNodeWithRandomPageRank)
        semanticTrustNetwork.addNodeToNodeNetworkNode(anotherNodeWithoutData)
        semanticTrustNetwork.addNodeToNodeNetworkEdge(
            randomNodeWithRandomPageRank,
            anotherNodeWithoutData,
            someNodeEdge
        )
        semanticTrustNetwork.addNodeToNodeNetworkEdge(
            yetAnotherNodeWithoutData,
            anotherNodeWithoutData,
            anotherNodeEdge
        )
        semanticTrustNetwork.addNodeToNodeNetworkEdge(
            anotherNodeWithoutData,
            randomNodeWithRandomPageRank,
            NodeTrustEdge(0.2, Timestamp(1))
        )
        semanticTrustNetwork.addNodeToNodeNetworkEdge(
            randomNodeWithRandomPageRank,
            yetAnotherNodeWithoutData,
            NodeTrustEdge(0.2, Timestamp(2))
        )
        val serializedGraph = semanticTrustNetwork.serializeCompactNodeNetwork()
        Assert.assertEquals(compactNodeToNodeGraph, serializedGraph)
    }

    @Test
    fun canSerializeAndDeserializeGraphWithTwoNodesAsJSON() {
        semanticTrustNetwork = TrustNetwork()
        semanticTrustNetwork.addNodeToNodeNetworkNode(randomNodeWithRandomPageRank)
        semanticTrustNetwork.addNodeToNodeNetworkNode(anotherNodeWithoutData)
        val serializedGraph = semanticTrustNetwork.serialize()
        val deserializedGraph = TrustNetwork.deserializeNodeToNodeJson(serializedGraph)
        Assert.assertEquals(serializedGraphWithTwoNodesString, serializedGraph)
        Assert.assertEquals(semanticTrustNetwork.nodeToNodeNetwork, deserializedGraph)
    }

    @Test
    fun canSerializeAndDeserializeGraphWithDataAndEdgesAsJSON() {
        semanticTrustNetwork = TrustNetwork()
        semanticTrustNetwork.addNodeToNodeNetworkNode(randomNodeWithRandomPageRank)
        semanticTrustNetwork.addNodeToNodeNetworkNode(anotherNodeWithoutData)
        semanticTrustNetwork.addNodeToNodeNetworkEdge(
            randomNodeWithRandomPageRank,
            anotherNodeWithoutData,
            someNodeEdge
        )
        val serializedGraph = semanticTrustNetwork.serialize()
        val deserializedGraph = TrustNetwork.deserializeNodeToNodeJson(serializedGraph)
        Assert.assertEquals(serializedNodeToNodeGraphWithSingleEdgeTwoNodes, serializedGraph)
        Assert.assertEquals(semanticTrustNetwork.nodeToNodeNetwork, deserializedGraph)
    }

    @Test
    fun canCreateANetworkFromSerializedJSONString() {
        semanticTrustNetwork = TrustNetwork()
        semanticTrustNetwork.addNodeToNodeNetworkNode(randomNodeWithRandomPageRank)
        semanticTrustNetwork.addNodeToNodeNetworkNode(anotherNodeWithoutData)
        val newNodeToNodeNetwork = TrustNetwork(serializedGraphWithTwoNodesString).nodeToNodeNetwork
        Assert.assertEquals(
            semanticTrustNetwork.nodeToNodeNetwork,
            newNodeToNodeNetwork
        )
        Assert.assertEquals(
            randomNodeWithRandomPageRank.personalisedPageRank,
            newNodeToNodeNetwork.vertexSet().first().personalisedPageRank,
            0.001
        )

    }

    @Test
    fun canCreateANetworkFromSerializedCompactString() {
        semanticTrustNetwork = TrustNetwork()
        semanticTrustNetwork.addNodeToNodeNetworkNode(yetAnotherNodeWithoutData)
        semanticTrustNetwork.addNodeToNodeNetworkNode(randomNodeWithRandomPageRank)
        semanticTrustNetwork.addNodeToNodeNetworkNode(anotherNodeWithoutData)
        semanticTrustNetwork.addNodeToNodeNetworkEdge(
            randomNodeWithRandomPageRank,
            anotherNodeWithoutData,
            someNodeEdge
        )
        semanticTrustNetwork.addNodeToNodeNetworkEdge(
            yetAnotherNodeWithoutData,
            anotherNodeWithoutData,
            anotherNodeEdge
        )
        semanticTrustNetwork.addNodeToNodeNetworkEdge(
            anotherNodeWithoutData,
            randomNodeWithRandomPageRank,
            NodeTrustEdge(0.2, Timestamp(1))
        )
        semanticTrustNetwork.addNodeToNodeNetworkEdge(
            randomNodeWithRandomPageRank,
            yetAnotherNodeWithoutData,
            NodeTrustEdge(0.2, Timestamp(2))
        )
        val newNodeToNodeNetwork = TrustNetwork.deserializeNodeToNodeCompact(compactNodeToNodeGraph)
        Assert.assertEquals(
            semanticTrustNetwork.nodeToNodeNetwork,
            newNodeToNodeNetwork
        )
    }

}
