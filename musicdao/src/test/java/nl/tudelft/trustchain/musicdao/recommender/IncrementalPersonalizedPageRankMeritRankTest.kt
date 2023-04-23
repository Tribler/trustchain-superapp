package nl.tudelft.trustchain.musicdao.recommender

import nl.tudelft.trustchain.musicdao.core.recommender.graph.NodeToNodeNetwork
import nl.tudelft.trustchain.musicdao.core.recommender.model.Node
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeTrustEdge
import nl.tudelft.trustchain.musicdao.core.recommender.ranking.IncrementalPersonalizedPageRank
import nl.tudelft.trustchain.musicdao.core.recommender.ranking.IncrementalPersonalizedPageRankMeritRank
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class IncrementalPersonalizedPageRankMeritRankTest {
    private var network: NodeToNodeNetwork = NodeToNodeNetwork()
    private lateinit var incrementalPageRankWithMeritRank: IncrementalPersonalizedPageRankMeritRank
    private val seed = 42L
    private lateinit var rootNode: Node
    private val rng = Random(seed)
    private val nNodes = 5000
    private val nEdges = 10
    private val repetitions = 10000
    private val maxWalkLength = 10000
    private val betaDecayThreshold = 0.5f
    private val betaDecay = 0.3f

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
        incrementalPageRankWithMeritRank = IncrementalPersonalizedPageRankMeritRank( maxWalkLength, repetitions, rootNode, 0.01f, betaDecayThreshold, betaDecay, network.graph)
        incrementalPageRankWithMeritRank.calculateRankings()
        Assert.assertEquals(0.0, rootNode.getPersonalizedPageRankScore(), 0.001)
        val rootNeighbors = network.getAllEdges().filter { network.graph.getEdgeSource(it) == rootNode }.map { network.graph.getEdgeTarget(it) }
        for(neighbour in rootNeighbors) {
            Assert.assertTrue(neighbour.getPersonalizedPageRankScore() > 0.0)
        }
    }

    @Test
    fun storesRandomWalks() {
        incrementalPageRankWithMeritRank = IncrementalPersonalizedPageRankMeritRank(maxWalkLength, repetitions, rootNode, betaDecayThreshold, betaDecay, 0.01f, network.graph)
        val randomWalks = incrementalPageRankWithMeritRank.randomWalks
        Assert.assertEquals(randomWalks.size, repetitions)
    }

    @Test
    fun decreasingResetProbabilityProportionatelyDecreasesChanceOfResetInRandomWalks() {
        incrementalPageRankWithMeritRank = IncrementalPersonalizedPageRankMeritRank( maxWalkLength, repetitions, rootNode, 0.01f, betaDecayThreshold, betaDecay, network.graph)
        val randomWalksWithHighResetProbability = incrementalPageRankWithMeritRank.randomWalks
        val nRandomWalksWithHighResetProbability = randomWalksWithHighResetProbability.flatten().count()
        incrementalPageRankWithMeritRank = IncrementalPersonalizedPageRankMeritRank( maxWalkLength, repetitions, rootNode, 0.001f, betaDecayThreshold, betaDecay, network.graph)
        val randomWalksWithLowerResetProbability = incrementalPageRankWithMeritRank.randomWalks
        val nRandomWalksWithLowerResetProbability = randomWalksWithLowerResetProbability.flatten().count()
        //The Reset Probability decreases by 10, so increase in random walk sizes should lie somewhere between 9 and 11 times
        Assert.assertTrue(nRandomWalksWithHighResetProbability * 9 < nRandomWalksWithLowerResetProbability)
        Assert.assertTrue(nRandomWalksWithHighResetProbability * 11 > nRandomWalksWithLowerResetProbability)
    }

    @Test
    fun onlyJumpsToNeighborsInRandomWalks() {
        incrementalPageRankWithMeritRank = IncrementalPersonalizedPageRankMeritRank( maxWalkLength, repetitions, rootNode, 0.01f, betaDecayThreshold, betaDecay, network.graph)
        val randomWalk = incrementalPageRankWithMeritRank.randomWalks
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
    fun canIncorporateModifiedEdgesAndIncrementallyRecalculatePageRankAccordingly() {
        incrementalPageRankWithMeritRank = IncrementalPersonalizedPageRankMeritRank( maxWalkLength, repetitions, rootNode, 0.01f, betaDecayThreshold, betaDecay, network.graph)
        incrementalPageRankWithMeritRank.calculateRankings()
        val rootNeighborEdges = network.getAllEdges().filter { network.graph.getEdgeSource(it) == rootNode }
        val rootNeighbors = rootNeighborEdges.map { network.graph.getEdgeTarget(it) }
        val randomNeighborEdge = rootNeighborEdges.first()
        val randomNeighbor = rootNeighbors.first()
        val randomNode = network.getAllNodes().first { !rootNeighbors.contains(it) && it != rootNode }
        val oldRandomNeighborPageRank = randomNeighbor.getPersonalizedPageRankScore()
        val oldRandomNodePageRank = randomNode.getPersonalizedPageRankScore()
        network.removeEdge(randomNeighborEdge)
        network.addEdge(rootNode, randomNode, NodeTrustEdge(randomNeighborEdge.trust))
        incrementalPageRankWithMeritRank.modifyEdges(setOf(rootNode))
        incrementalPageRankWithMeritRank.calculateRankings()
        Assert.assertTrue(randomNeighbor.getPersonalizedPageRankScore() < oldRandomNeighborPageRank)
        Assert.assertTrue(randomNode.getPersonalizedPageRankScore() > oldRandomNodePageRank)
    }

    @Test
    fun betaDecaysEnsureThatEntirelySybilNodeReceivesLowerScore() {
        val sybilNode1 = Node("sybil1")
        val sybilNode2 = Node("sybil2")
        val sybilNode3 = Node("sybil3")
        network.addNode(sybilNode1)
        network.addNode(sybilNode2)
        network.addNode(sybilNode3)
        //select attack vector from trusted neighbor to conduct cycle attack
        val rootNeighborEdges = network.getAllEdges().filter { network.graph.getEdgeSource(it) == rootNode }.sortedBy { it.trust }
        val rootNeighbors = rootNeighborEdges.map { network.graph.getEdgeTarget(it) }
        val attacker = rootNeighbors.last()
        val existingNeighborsOfAttacker = network.getAllEdges().filter { network.graph.getEdgeSource(it) == attacker }
        for(existingNeighborEdge in existingNeighborsOfAttacker) {
            network.removeEdge(existingNeighborEdge)
        }
        network.addEdge(attacker, sybilNode1, NodeTrustEdge(1.0 ))
        network.addEdge(sybilNode1, sybilNode2, NodeTrustEdge(1.0))
        network.addEdge(sybilNode2, sybilNode3, NodeTrustEdge(1.0))

        //first conduct attack without beta decay
        val incrementalPageRank = IncrementalPersonalizedPageRank( maxWalkLength, repetitions, rootNode, 0.01f, network.graph)
        incrementalPageRank.calculateRankings()

        val sybilNode1ScoreWithoutBetaDecay = sybilNode1.getPersonalizedPageRankScore()
        val sybilNode2ScoreWithoutBetaDecay = sybilNode2.getPersonalizedPageRankScore()
        val sybilNode3ScoreWithoutBetaDecay = sybilNode3.getPersonalizedPageRankScore()

        incrementalPageRankWithMeritRank = IncrementalPersonalizedPageRankMeritRank( maxWalkLength, repetitions, rootNode, 0.01f, betaDecayThreshold, betaDecay, network.graph)
        incrementalPageRankWithMeritRank.calculateRankings()

        val sybilNode1ScoreWithBetaDecay = sybilNode1.getPersonalizedPageRankScore()
        val sybilNode2ScoreWithBetaDecay = sybilNode2.getPersonalizedPageRankScore()
        val sybilNode3ScoreWithBetaDecay = sybilNode3.getPersonalizedPageRankScore()

        Assert.assertTrue(sybilNode1ScoreWithoutBetaDecay > sybilNode1ScoreWithBetaDecay)
        Assert.assertTrue(sybilNode2ScoreWithoutBetaDecay > sybilNode2ScoreWithBetaDecay)
        Assert.assertTrue(sybilNode3ScoreWithoutBetaDecay > sybilNode3ScoreWithBetaDecay)
    }

}
