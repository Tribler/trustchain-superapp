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
        incrementalPageRank = IncrementalPersonalizedPageRank(mutableListOf(), 1000, 10000, 0.01f, rootNode)
        incrementalPageRank.initiateRandomWalks(network.graph, seed)
        incrementalPageRank.calculatePersonalizedPageRank(network.graph)
//        Assert.assertEquals(rootNode.personalisedPageRank, 0)
    }

}
