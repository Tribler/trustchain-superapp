package nl.tudelft.trustchain.musicdao.recommender.experiments

import nl.tudelft.trustchain.musicdao.core.recommender.collaborativefiltering.UserBasedTrustedCollaborativeFiltering
import nl.tudelft.trustchain.musicdao.core.recommender.gossip.EdgeGossiper
import nl.tudelft.trustchain.musicdao.core.recommender.graph.*
import nl.tudelft.trustchain.musicdao.core.recommender.model.*
import nl.tudelft.trustchain.musicdao.core.recommender.ranking.IncrementalHybridPersonalizedPageRankSalsa
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import kotlin.random.Random

class SybilResistance {
    private lateinit var trustNetwork: SongRecTrustNetwork
    private lateinit var incrementalHybrid: IncrementalHybridPersonalizedPageRankSalsa
    private val seed = 42L
    private lateinit var rootNode: Node
    private val rng = Random(seed)
    private val nNodes = 5000
    private val nSongs = nNodes / 10
    private val nEdges = 10
    private val maxTimestamp = System.currentTimeMillis() + 10000
    private val minTimestamp = System.currentTimeMillis()
    private val contextDir = "src/test/resources"
    private lateinit var edgeGossiper: EdgeGossiper

    @Before
    fun setUp() {
        val usersFile = File("$contextDir/dataset/kaggle_users.txt")
        trustNetwork = SongRecTrustNetwork(usersFile.bufferedReader().use { it.readLine() })
        val songsFile = File("$contextDir/dataset/kaggle_songs.txt")
        val interactionFile = File("$contextDir/dataset/kaggle_visible_evaluation_triplets.txt")
        try {
            BufferedReader(FileReader(usersFile)).use { br ->
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    trustNetwork.addNode(Node(line!!))
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        try {
            BufferedReader(FileReader(songsFile)).use { br ->
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    val words = line?.split("\\s".toRegex())?.toTypedArray()
                    trustNetwork.addSongRec(SongRecommendation(words!!.first()))
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        try {
            BufferedReader(FileReader(songsFile)).use { br ->
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    val words = line?.split("\\s".toRegex())?.toTypedArray()
                    trustNetwork.addSongRec(SongRecommendation(words!!.first()))
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        try {
            BufferedReader(FileReader(interactionFile)).use { br ->
                var line: String?
                var lastNode: Node = interactionFile.bufferedReader().use { Node(it.readLine().split("\\s".toRegex()).toTypedArray().first()) }
                var songRecAndListenCount: MutableMap<SongRecommendation, Int> = mutableMapOf()
                while (br.readLine().also { line = it } != null) {
                    val words = line?.split("\\s".toRegex())?.toTypedArray()
                    if(Node(words!!.first()) != lastNode) {
                        val totalCount = songRecAndListenCount.values.sum()
                        val nodeToSongEdges = mutableListOf<NodeSongEdgeWithNodeAndSongRec>()
                        for((rec, count) in songRecAndListenCount) {
                            val affinity = count.toDouble() / totalCount
                            nodeToSongEdges.add(NodeSongEdgeWithNodeAndSongRec(NodeSongEdge(affinity), lastNode, rec))
                        }
                        trustNetwork.bulkAddNodeToSongEdgesForExperiments(nodeToSongEdges, lastNode)
                        songRecAndListenCount = mutableMapOf()
                        lastNode = Node(words.first())
                    }
                    songRecAndListenCount[SongRecommendation(words[1])] = words[2].toInt()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        for(node in trustNetwork.nodeToNodeNetwork.getAllNodes()) {
            val nTSGraph = trustNetwork.nodeToSongNetwork.graph
            val nodeSongEdges = nTSGraph.outgoingEdgesOf(node).map { NodeSongEdgeWithNodeAndSongRec(it, node, nTSGraph.getEdgeTarget(it) as SongRecommendation) }
            val nodeSongs = nodeSongEdges.map { it.songRec }
            if(nodeSongs.isEmpty())
                continue
            val commonItemUsers = trustNetwork.nodeToNodeNetwork.getAllNodes().filter {
                nTSGraph.outgoingEdgesOf(it).filter { nodeSongs.contains(nTSGraph.getEdgeTarget(it)) }.size > 1
            }.filter { it != node }
            if(commonItemUsers.isEmpty())
                continue
            val cf = UserBasedTrustedCollaborativeFiltering(commonItemUsers, trustNetwork, 0.0, 0.5)
            val commonUsers = cf.similarNodes(nodeSongEdges, 5)
            for((similarNode, score) in commonUsers) {
                trustNetwork.addNodeToNodeEdge(NodeTrustEdgeWithSourceAndTarget(NodeTrustEdge(score), node, similarNode))
            }
        }
        trustNetwork.resetRandomWalks()
    }

    @Test
    fun measureSybilResistanceForAlphaDecays() {
        print("bla")
    }


}
