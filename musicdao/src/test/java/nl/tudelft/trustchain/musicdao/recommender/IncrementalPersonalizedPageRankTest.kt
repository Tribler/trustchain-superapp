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
    private val repetitions = 10000
    private val maxWalkLength = 10000

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
                val randomNode = if(randomNum < node.getIpv8().toInt()) randomNum else randomNum + 1
                network.addEdge(node, allNodes[randomNode], NodeTrustEdge(rng.nextDouble()))
            }
        }
    }

    @Test
    fun canCaclulatePersonalizedPageRank() {
        incrementalPageRank = IncrementalPersonalizedPageRank( maxWalkLength, repetitions, rootNode, 0.01f, network.graph)
        incrementalPageRank.calculateRankings()
        Assert.assertEquals(0.0, rootNode.getPersonalizedPageRankScore(), 0.001)
        val rootNeighbors = network.getAllEdges().filter { network.graph.getEdgeSource(it) == rootNode }.map { network.graph.getEdgeTarget(it) }
        for(neighbour in rootNeighbors) {
            Assert.assertTrue(neighbour.getPersonalizedPageRankScore() > 0.0)
        }
    }

    @Test
    fun storesRandomWalks() {
        incrementalPageRank = IncrementalPersonalizedPageRank(maxWalkLength, repetitions, rootNode, 0.01f, network.graph)
        val randomWalks = incrementalPageRank.randomWalks
        Assert.assertEquals(randomWalks.size, repetitions)
    }

    @Test
    fun decreasingResetProbabilityProportionatelyDecreasesChanceOfResetInRandomWalks() {
        incrementalPageRank = IncrementalPersonalizedPageRank( maxWalkLength, repetitions, rootNode, 0.01f, network.graph)
        val randomWalksWithHighResetProbability = incrementalPageRank.randomWalks
        val nRandomWalksWithHighResetProbability = randomWalksWithHighResetProbability.flatten().count()
        incrementalPageRank = IncrementalPersonalizedPageRank( maxWalkLength, repetitions, rootNode, 0.001f, network.graph)
        val randomWalksWithLowerResetProbability = incrementalPageRank.randomWalks
        val nRandomWalksWithLowerResetProbability = randomWalksWithLowerResetProbability.flatten().count()
        //The Reset Probability decreases by 10, so increase in random walk sizes should lie somewhere between 9 and 11 times
        Assert.assertTrue(nRandomWalksWithHighResetProbability * 9 < nRandomWalksWithLowerResetProbability)
        Assert.assertTrue(nRandomWalksWithHighResetProbability * 11 > nRandomWalksWithLowerResetProbability)
    }

    @Test
    fun onlyJumpsToNeighborsInRandomWalks() {
        incrementalPageRank = IncrementalPersonalizedPageRank( maxWalkLength, repetitions, rootNode, 0.01f, network.graph)
        val randomWalk = incrementalPageRank.randomWalks
        val randomIndex = rng.nextInt(0, randomWalk.size - 1)
        val randomlySelectedWalk = randomWalk.get(randomIndex)
        Assert.assertEquals(rootNode, randomlySelectedWalk[0])
        var lastNode = rootNode
        for(i in 1 until randomlySelectedWalk.size) {
            val neighborEdges = network.getAllEdges().filter { network.graph.getEdgeSource(it) == lastNode }
            val neighborsOfLastNode = neighborEdges.map { network.graph.getEdgeTarget(it) }
            Assert.assertTrue(neighborsOfLastNode.contains(randomlySelectedWalk[i]))
            lastNode = randomlySelectedWalk[i]
        }
    }

    @Test
    fun canIncorporateModifiedEdgesAndRecalculatePageRankAccordingly() {
        incrementalPageRank = IncrementalPersonalizedPageRank( maxWalkLength, repetitions, rootNode, 0.01f, network.graph)
        incrementalPageRank.calculateRankings()
        val rootNeighborEdges = network.getAllEdges().filter { network.graph.getEdgeSource(it) == rootNode }
        val rootNeighbors = rootNeighborEdges.map { network.graph.getEdgeTarget(it) }
        val randomNeighborEdge = rootNeighborEdges.first()
        val randomNeighbor = rootNeighbors.first()
        val randomNode = network.getAllNodes().first { !rootNeighbors.contains(it) && it != rootNode }
        val oldRandomNeighborPageRank = randomNeighbor.getPersonalizedPageRankScore()
        val oldRandomNodePageRank = randomNode.getPersonalizedPageRankScore()
        network.removeEdge(randomNeighborEdge)
        network.addEdge(rootNode, randomNode, NodeTrustEdge(randomNeighborEdge.trust))
        incrementalPageRank.modifyEdges(setOf(rootNode))
        incrementalPageRank.calculateRankings()
        Assert.assertTrue(randomNeighbor.getPersonalizedPageRankScore() < oldRandomNeighborPageRank)
        Assert.assertTrue(randomNode.getPersonalizedPageRankScore() > oldRandomNodePageRank)
    }

}
