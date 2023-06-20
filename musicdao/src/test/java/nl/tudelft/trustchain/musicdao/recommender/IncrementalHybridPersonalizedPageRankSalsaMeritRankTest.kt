package nl.tudelft.trustchain.musicdao.recommender

import nl.tudelft.trustchain.musicdao.core.recommender.graph.NodeToNodeNetwork
import nl.tudelft.trustchain.musicdao.core.recommender.graph.NodeToSongNetwork
import nl.tudelft.trustchain.musicdao.core.recommender.model.Node
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeSongEdge
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeTrustEdge
import nl.tudelft.trustchain.musicdao.core.recommender.model.Recommendation
import nl.tudelft.trustchain.musicdao.core.recommender.networks.TrustNetwork
import nl.tudelft.trustchain.musicdao.core.recommender.ranking.*
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.sql.Timestamp
import kotlin.random.Random

class IncrementalHybridPersonalizedPageRankSalsaMeritRankTest {
    private lateinit var nodeToSongNetwork: NodeToSongNetwork
    private lateinit var nodeToNodeNetwork: NodeToNodeNetwork
    private lateinit var incrementalHybridPersonalizedPageRankSalsaMeritRank: IncrementalHybridPersonalizedPageRankSalsaMeritRank
    private val seed = 42L
    private lateinit var rootNode: Node
    private val rng = Random(seed)
    private val nNodes = 500
    private val nSongs = nNodes
    private val nEdges = 10
    private val maxTimestamp = System.currentTimeMillis() + 10000
    private val minTimestamp = System.currentTimeMillis()
    private val repetitions = 10000
    private val maxWalkLength = 10000
    private val betaDecayThreshold = TrustNetwork.BETA_DECAY_THRESHOLD

    @Before
    fun setUp() {
        nodeToNodeNetwork = NodeToNodeNetwork()
        nodeToSongNetwork = NodeToSongNetwork()
        for(node in 0 until nNodes) {
            val nodeToAdd = Node(node.toString())
            nodeToNodeNetwork.addNode(nodeToAdd)
            nodeToSongNetwork.addNodeOrSong(nodeToAdd)
        }
        for(song in 0 until nSongs) {
            nodeToSongNetwork.addNodeOrSong(Recommendation(song.toString()))
        }
        // Create 10 edges from each node to 10 random songs
        val allNodes = nodeToSongNetwork.getAllNodes().toList()
        rootNode = allNodes[0]
        val allSongs = nodeToSongNetwork.getAllSongs().toList()
        for(node in allNodes) {
            for(i in 0 until nEdges) {
                var randomNum = (0 until nSongs - 1).random(rng)
                nodeToSongNetwork.addEdge(node, allSongs[randomNum], NodeSongEdge(rng.nextDouble(), Timestamp(rng.nextLong(minTimestamp, maxTimestamp))))
                randomNum = (0 until nNodes - 1).random(rng)
                val randomNode = if(randomNum < node.getKey().toInt()) randomNum else randomNum + 1
                nodeToNodeNetwork.addEdge(node, allNodes[randomNode], NodeTrustEdge(rng.nextDouble(), Timestamp(rng.nextLong(minTimestamp, maxTimestamp))))
            }
        }
    }

    @Test
    fun betaDecaysEnsureThatEntirelySybilNodeReceivesLowerScore() {
        val sybilNode1 = Node("sybil1")
        val sybilNode2 = Node("sybil2")
        val sybilNode3 = Node("sybil3")
        nodeToNodeNetwork.addNode(sybilNode1)
        nodeToNodeNetwork.addNode(sybilNode2)
        nodeToNodeNetwork.addNode(sybilNode3)
        nodeToSongNetwork.addNodeOrSong(sybilNode1)
        nodeToSongNetwork.addNodeOrSong(sybilNode2)
        nodeToSongNetwork.addNodeOrSong(sybilNode3)
        val sybilSong1 = Recommendation("sybil1")
        val sybilSong2 = Recommendation("sybil2")
        val sybilSong3 = Recommendation("sybil3")
        nodeToSongNetwork.addNodeOrSong(sybilSong1)
        nodeToSongNetwork.addNodeOrSong(sybilSong2)
        nodeToSongNetwork.addNodeOrSong(sybilSong3)
        //select attack vector from trusted neighbor to conduct cycle attack
        val rootNeighborEdges = nodeToNodeNetwork.getAllEdges().filter { nodeToNodeNetwork.graph.getEdgeSource(it) == rootNode }.sortedBy { it.trust }
        val rootNeighbors = rootNeighborEdges.map { nodeToNodeNetwork.graph.getEdgeTarget(it) }
        val attacker = rootNeighbors.last()
        val existingNeighborsOfAttacker = nodeToNodeNetwork.getAllEdges().filter { nodeToNodeNetwork.graph.getEdgeSource(it) == attacker }
        for(existingNeighborEdge in existingNeighborsOfAttacker) {
            nodeToNodeNetwork.removeEdge(existingNeighborEdge)
        }
        nodeToNodeNetwork.addEdge(attacker, sybilNode1, NodeTrustEdge(1.0 ))
        nodeToNodeNetwork.addEdge(sybilNode1, sybilNode2, NodeTrustEdge(1.0))
        nodeToNodeNetwork.addEdge(sybilNode2, sybilNode3, NodeTrustEdge(1.0))
        nodeToSongNetwork.addEdge(attacker, sybilSong1, NodeSongEdge(1.0))
        nodeToSongNetwork.addEdge(attacker, sybilSong2, NodeSongEdge(1.0))
        nodeToSongNetwork.addEdge(attacker, sybilSong3, NodeSongEdge(1.0))
        nodeToSongNetwork.addEdge(sybilNode1, sybilSong1, NodeSongEdge(1.0))
        nodeToSongNetwork.addEdge(sybilNode1, sybilSong2, NodeSongEdge(1.0))
        nodeToSongNetwork.addEdge(sybilNode1, sybilSong3, NodeSongEdge(1.0))
        nodeToSongNetwork.addEdge(sybilNode2, sybilSong1, NodeSongEdge(1.0))
        nodeToSongNetwork.addEdge(sybilNode2, sybilSong2, NodeSongEdge(1.0))
        nodeToSongNetwork.addEdge(sybilNode2, sybilSong3, NodeSongEdge(1.0))
        nodeToSongNetwork.addEdge(sybilNode3, sybilSong1, NodeSongEdge(1.0))
        nodeToSongNetwork.addEdge(sybilNode3, sybilSong2, NodeSongEdge(1.0))
        nodeToSongNetwork.addEdge(sybilNode3, sybilSong3, NodeSongEdge(1.0))
        //first conduct attack without beta decay
        val incrementalPageRank = IncrementalPersonalizedPageRank( maxWalkLength, repetitions, rootNode, 0.01, nodeToNodeNetwork.graph)
        incrementalPageRank.calculateRankings()

        val incrementalHybridPageRank = IncrementalHybridPersonalizedPageRankSalsaMeritRank(maxWalkLength, repetitions, rootNode, 0.01, 0.0, betaDecayThreshold, 0.0, nodeToSongNetwork.graph, nodeToNodeNetwork.graph)
        incrementalHybridPageRank.calculateRankings()

        val sybilSong1ScoreWithoutBetaDecay = sybilSong1.getRecommendationScore()
        val sybilSong2ScoreWithoutBetaDecay = sybilSong2.getRecommendationScore()
        val sybilSong3ScoreWithoutBetaDecay = sybilSong3.getRecommendationScore()

        incrementalHybridPersonalizedPageRankSalsaMeritRank = IncrementalHybridPersonalizedPageRankSalsaMeritRank(maxWalkLength, repetitions, rootNode, 0.01, 0.1, betaDecayThreshold, 0.0, nodeToSongNetwork.graph, nodeToNodeNetwork.graph)
        incrementalHybridPersonalizedPageRankSalsaMeritRank.calculateRankings()

        val sybilSong1ScoreWithBetaDecay = sybilSong1.getRecommendationScore()
        val sybilSong2ScoreWithBetaDecay = sybilSong2.getRecommendationScore()
        val sybilSong3ScoreWithBetaDecay = sybilSong3.getRecommendationScore()

        Assert.assertTrue(sybilSong1ScoreWithoutBetaDecay > sybilSong1ScoreWithBetaDecay)
        Assert.assertTrue(sybilSong2ScoreWithoutBetaDecay > sybilSong2ScoreWithBetaDecay)
        Assert.assertTrue(sybilSong3ScoreWithoutBetaDecay > sybilSong3ScoreWithBetaDecay)

        incrementalHybridPersonalizedPageRankSalsaMeritRank = IncrementalHybridPersonalizedPageRankSalsaMeritRank(maxWalkLength, repetitions, rootNode, 0.01, 0.0, betaDecayThreshold, 0.0, nodeToSongNetwork.graph, nodeToNodeNetwork.graph)
        incrementalHybridPersonalizedPageRankSalsaMeritRank.calculateRankings()

        val sybilSong1ScoreWithZeroBetaDecay = sybilSong1.getRecommendationScore()
        val sybilSong2ScoreWithZeroBetaDecay = sybilSong2.getRecommendationScore()
        val sybilSong3ScoreWithZeroBetaDecay = sybilSong3.getRecommendationScore()

        Assert.assertTrue(sybilSong1ScoreWithZeroBetaDecay > sybilSong1ScoreWithBetaDecay)
        Assert.assertTrue(sybilSong2ScoreWithZeroBetaDecay > sybilSong2ScoreWithBetaDecay)
        Assert.assertTrue(sybilSong3ScoreWithZeroBetaDecay > sybilSong3ScoreWithBetaDecay)
    }

    @Test
    fun scoreForSongsReflectsPreferencesOfUsers() {
        incrementalHybridPersonalizedPageRankSalsaMeritRank = IncrementalHybridPersonalizedPageRankSalsaMeritRank(maxWalkLength, repetitions, rootNode, 0.01, 0.0, betaDecayThreshold, 0.0, nodeToSongNetwork.graph, nodeToNodeNetwork.graph)
        incrementalHybridPersonalizedPageRankSalsaMeritRank.calculateRankings()
        val allSongEdges = nodeToSongNetwork.getAllEdges()
        val rootSongsSorted = allSongEdges.filter { nodeToSongNetwork.graph.getEdgeSource(it) == rootNode }.sortedBy { it.affinity }.map { nodeToSongNetwork.graph.getEdgeTarget(it) }
        Assert.assertTrue(rootSongsSorted.first().rankingScore < rootSongsSorted.last().rankingScore)
    }


}
