package nl.tudelft.trustchain.musicdao.recommender

import nl.tudelft.trustchain.musicdao.core.recommender.graph.NodeToNodeNetwork
import nl.tudelft.trustchain.musicdao.core.recommender.model.Node
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeTrustEdge
import nl.tudelft.trustchain.musicdao.core.recommender.ranking.IncrementalPersonalizedPageRank
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class IncrementalPersonalizedPageRankTest {
    private var network: NodeToNodeNetwork = NodeToNodeNetwork()
    private lateinit var incrementalPageRank: IncrementalPersonalizedPageRank
    private val seed = 42L
    private lateinit var rootNode: Node
    private val rng = Random(seed)
    private val nNodes = 5000
    private val nEdges = 10

    @Before
    fun setUp() {
        for(node in 0 until nNodes) {
            network.addNode(Node(node.toString()))
        }
        // Create 10 edges from each node to 10 other nodes
        val allNodes = network.getAllNodes().toList()
        rootNode = allNodes[0]
        for(node in allNodes) {
            for(i in 0 until nEdges) {
                val randomNum = (0 until nNodes - 1).random(rng)
                val randomNode = if(randomNum < node.ipv8.toInt()) randomNum else randomNum + 1
                network.addEdge(node, allNodes[randomNode], NodeTrustEdge(rng.nextDouble()))
            }
        }
    }

    @Test
    fun canCaclulatePersonalizedPageRank() {
        incrementalPageRank = IncrementalPersonalizedPageRank( 1000, 10000, rootNode, 0.01f, network.graph)
        incrementalPageRank.initiateRandomWalks()
        incrementalPageRank.calculatePersonalizedPageRank()
        Assert.assertEquals(0.0, rootNode.personalisedPageRank, 0.001)
        val rootNeighbors = network.getAllNodeToNodeNetworkEdges().filter { network.graph.getEdgeSource(it) == rootNode }.map { network.graph.getEdgeTarget(it) }
        for(neighbour in rootNeighbors) {
            Assert.assertTrue(neighbour.personalisedPageRank > 0.0)
        }
    }

    @Test
    fun canIncorporateModifiedEdgesAndRecalculatePageRankAccordingly() {
        incrementalPageRank = IncrementalPersonalizedPageRank( 1000, 10000, rootNode, 0.01f, network.graph)
        incrementalPageRank.initiateRandomWalks()
        incrementalPageRank.calculatePersonalizedPageRank()
        val rootNeighborEdges = network.getAllNodeToNodeNetworkEdges().filter { network.graph.getEdgeSource(it) == rootNode }
        val rootNeighbors = rootNeighborEdges.map { network.graph.getEdgeTarget(it) }
        val randomNeighborEdge = rootNeighborEdges.first()
        val randomNeighbor = rootNeighbors.first()
        val randomNode = network.getAllNodes().first { !rootNeighbors.contains(it) && it != rootNode }
        val oldRandomNeighborPageRank = randomNeighbor.personalisedPageRank
        val oldRandomNodePageRank = randomNode.personalisedPageRank
        network.removeEdge(randomNeighborEdge)
        network.addEdge(rootNode, randomNode, NodeTrustEdge(randomNeighborEdge.trust))
        incrementalPageRank.modifyEdges(setOf(rootNode))
        incrementalPageRank.calculatePersonalizedPageRank()
        Assert.assertTrue(randomNeighbor.personalisedPageRank < oldRandomNeighborPageRank)
        Assert.assertTrue(randomNode.personalisedPageRank > oldRandomNodePageRank)
    }



}
