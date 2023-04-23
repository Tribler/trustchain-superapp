package nl.tudelft.trustchain.musicdao.recommender

import nl.tudelft.trustchain.musicdao.core.recommender.graph.NodeToNodeNetwork
import nl.tudelft.trustchain.musicdao.core.recommender.graph.NodeToSongNetwork
import nl.tudelft.trustchain.musicdao.core.recommender.model.Node
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeSongEdge
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeTrustEdge
import nl.tudelft.trustchain.musicdao.core.recommender.model.SongRecommendation
import nl.tudelft.trustchain.musicdao.core.recommender.ranking.IncrementalHybridPersonalizedPageRankSalsa
import nl.tudelft.trustchain.musicdao.core.recommender.ranking.IncrementalPersonalizedPageRank
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class IncrementalHybridPersonalizedPageRankSalsaTest {
    private var nodeToSongNetwork: NodeToSongNetwork = NodeToSongNetwork()
    private var nodeToNodeNetwork: NodeToNodeNetwork = NodeToNodeNetwork()
    private lateinit var incrementalHybrid: IncrementalHybridPersonalizedPageRankSalsa
    private val seed = 42L
    private lateinit var rootNode: Node
    private val rng = Random(seed)
    private val nNodes = 5000
    private val nSongs = nNodes / 10
    private val nEdges = 10
    private val repetitions = 40000
    private val maxWalkLength = 1000
    private val explorationProbability = 0.05f

    @Before
    fun setUp() {
        for(node in 0 until nNodes) {
            val nodeToAdd = Node(node.toString())
            nodeToNodeNetwork.addNode(nodeToAdd)
            nodeToSongNetwork.addNodeOrSong(nodeToAdd)
        }
        for(song in 0 until nSongs) {
            nodeToSongNetwork.addNodeOrSong(SongRecommendation(song.toString()))
        }
        // Create 10 edges from each node to 10 random songs
        val allNodes = nodeToSongNetwork.getAllNodes().toList()
        rootNode = allNodes[0]
        val allSongs = nodeToSongNetwork.getAllSongs().toList()
        for(node in allNodes) {
            for(i in 0 until nEdges) {
                var randomNum = (0 until nSongs - 1).random(rng)
                nodeToSongNetwork.addEdge(node, allSongs[randomNum], NodeSongEdge(rng.nextDouble()))
                randomNum = (0 until nNodes - 1).random(rng)
                val randomNode = if(randomNum < node.getIpv8().toInt()) randomNum else randomNum + 1
                nodeToNodeNetwork.addEdge(node, allNodes[randomNode], NodeTrustEdge(rng.nextDouble()))
            }
        }
    }

    @Test
    fun canCaclulateScoreForSongs() {
        incrementalHybrid = IncrementalHybridPersonalizedPageRankSalsa(maxWalkLength, repetitions, rootNode, 0.01f, explorationProbability, nodeToSongNetwork.graph)
        incrementalHybrid.calculateRankings()
        Assert.assertEquals(1.0, nodeToSongNetwork.getAllSongs().map { it.rankingScore }.sum(), 0.001)
    }

    @Test
    fun scoreForSongsReflectsPreferencesOfUsers() {
        incrementalHybrid = IncrementalHybridPersonalizedPageRankSalsa(maxWalkLength, repetitions, rootNode, 0.01f, explorationProbability, nodeToSongNetwork.graph)
        incrementalHybrid.calculateRankings()
        val allSongEdges = nodeToSongNetwork.getAllEdges()
        val rootSongsSorted = allSongEdges.filter { nodeToSongNetwork.graph.getEdgeSource(it) == rootNode }.sortedBy { it.affinity }.map { nodeToSongNetwork.graph.getEdgeTarget(it) }
        Assert.assertTrue(rootSongsSorted.first().rankingScore < rootSongsSorted.last().rankingScore)
    }

    @Test
    fun scoreForSongsReflectsTrustInNeighbors() {
        val pageRank = IncrementalPersonalizedPageRank(maxWalkLength, repetitions, rootNode, 0.05f, nodeToNodeNetwork.graph)
        pageRank.calculateRankings()
        incrementalHybrid = IncrementalHybridPersonalizedPageRankSalsa(maxWalkLength, repetitions, rootNode, 0.05f, explorationProbability, nodeToSongNetwork.graph)
        incrementalHybrid.calculateRankings()
        val allNodeToNodeEdges = nodeToNodeNetwork.getAllEdges()
        val allNeighborsSortedByTrust = allNodeToNodeEdges.filter { nodeToNodeNetwork.graph.getEdgeSource(it) == rootNode }.map { nodeToNodeNetwork.graph.getEdgeTarget(it) }.sortedBy { it.getPersonalizedPageRankScore() }
        val leastTrustedNeighbor = allNeighborsSortedByTrust.first()
        val mostTrustedNeighbor = allNeighborsSortedByTrust.last()
        val allSongEdges = nodeToSongNetwork.getAllEdges()
        val leastTrustedNeighborSongsWeight = allSongEdges.filter { nodeToSongNetwork.graph.getEdgeSource(it) == leastTrustedNeighbor }.sortedBy { it.affinity }.map { nodeToSongNetwork.graph.getEdgeTarget(it).rankingScore }.sum()
        val mostTrustedNeighborSongsWeight = allSongEdges.filter { nodeToSongNetwork.graph.getEdgeSource(it) == mostTrustedNeighbor }.sortedBy { it.affinity }.map { nodeToSongNetwork.graph.getEdgeTarget(it).rankingScore }.sum()
        Assert.assertTrue(leastTrustedNeighborSongsWeight < mostTrustedNeighborSongsWeight)
    }

    @Test
    fun canModifyNodeToSongEdgesWhichIncrementallyAltersSongRanking() {
        val pageRank = IncrementalPersonalizedPageRank(maxWalkLength, repetitions, rootNode, 0.01f, nodeToNodeNetwork.graph)
        pageRank.calculateRankings()
        incrementalHybrid = IncrementalHybridPersonalizedPageRankSalsa(maxWalkLength, repetitions, rootNode, 0.01f, explorationProbability, nodeToSongNetwork.graph)
        incrementalHybrid.calculateRankings()
        val rootSongEdges = nodeToSongNetwork.getAllEdges().filter { nodeToSongNetwork.graph.getEdgeSource(it) == rootNode }
        val rootSongs = rootSongEdges.map { nodeToSongNetwork.graph.getEdgeTarget(it) }
        val randomRootSongEdge = rootSongEdges.first()
        val randomRootSong = rootSongs.first()
        val randomNonRootSong = nodeToSongNetwork.getAllSongs().first { !rootSongs.contains(it) }
        Assert.assertTrue(randomNonRootSong.rankingScore < randomRootSong.rankingScore)
        val oldRootSongRank = randomRootSong.rankingScore
        val oldNonRootSongScore = randomNonRootSong.rankingScore
        nodeToSongNetwork.removeEdge(randomRootSongEdge)
        nodeToSongNetwork.addEdge(rootNode, randomNonRootSong, NodeSongEdge(randomRootSongEdge.affinity))
        incrementalHybrid.modifyNodesOrSongs(setOf(rootNode), nodeToNodeNetwork.getAllNodes().toList())
        incrementalHybrid.calculateRankings()
        Assert.assertTrue(randomRootSong.rankingScore < oldRootSongRank)
        Assert.assertTrue(randomNonRootSong.rankingScore > oldNonRootSongScore)
        Assert.assertTrue(randomNonRootSong.rankingScore > randomRootSong.rankingScore)
    }

}
