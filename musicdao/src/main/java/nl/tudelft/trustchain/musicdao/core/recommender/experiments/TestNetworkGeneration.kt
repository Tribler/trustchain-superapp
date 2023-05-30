package nl.tudelft.trustchain.musicdao.core.recommender.experiments

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nl.tudelft.trustchain.musicdao.core.recommender.collaborativefiltering.UserBasedTrustedCollaborativeFiltering
import nl.tudelft.trustchain.musicdao.core.recommender.model.*
import nl.tudelft.trustchain.musicdao.core.recommender.networks.SongRecTrustNetwork
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException

fun main() {
    lateinit var trustNetwork: SongRecTrustNetwork
    val currentDir = System.getProperty("user.dir")
    val contextDir = "$currentDir/musicdao/src/test/resources"
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
                trustNetwork.addSongRec(Recommendation(words!!.first()))
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
                trustNetwork.addSongRec(Recommendation(words!!.first()))
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
    try {
        BufferedReader(FileReader(interactionFile)).use { br ->
            var line: String?
            var lastNode: Node = interactionFile.bufferedReader()
                .use { Node(it.readLine().split("\\s".toRegex()).toTypedArray().first()) }
            var songRecAndListenCount: MutableMap<Recommendation, Int> = mutableMapOf()
            while (br.readLine().also { line = it } != null) {
                val words = line?.split("\\s".toRegex())?.toTypedArray()
                if (Node(words!!.first()) != lastNode) {
                    val totalCount = songRecAndListenCount.values.sum()
                    val nodeToSongEdges = mutableListOf<NodeRecEdge>()
                    for ((rec, count) in songRecAndListenCount) {
                        val affinity = count.toDouble() / totalCount
                        nodeToSongEdges.add(NodeRecEdge(NodeSongEdge(affinity), lastNode, rec))
                    }
                    trustNetwork.bulkAddNodeToSongEdgesForExperiments(nodeToSongEdges, lastNode)
                    songRecAndListenCount = mutableMapOf()
                    lastNode = Node(words.first())
                }
                songRecAndListenCount[Recommendation(words[1])] = words[2].toInt()
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
    val totalNodes = trustNetwork.nodeToNodeNetwork.getAllNodes().size
    for ((nodesCompleted, node) in trustNetwork.nodeToNodeNetwork.getAllNodes().withIndex()) {
        println("$nodesCompleted/$totalNodes")
        val nTSGraph = trustNetwork.nodeToSongNetwork.graph
        val nodeSongEdges = nTSGraph.outgoingEdgesOf(node)
            .map { NodeRecEdge(it, node, nTSGraph.getEdgeTarget(it) as Recommendation) }
        val nodeSongs = nodeSongEdges.map { it.rec }
        if (nodeSongs.isEmpty())
            continue
        val commonItemUsers = trustNetwork.nodeToNodeNetwork.getAllNodes().filter {
            nTSGraph.outgoingEdgesOf(it).filter { nodeSongs.contains(nTSGraph.getEdgeTarget(it)) }.size > 1
        }.filter { it != node }
        if (commonItemUsers.isEmpty())
            continue
        val cf = UserBasedTrustedCollaborativeFiltering(commonItemUsers, trustNetwork, 0.0, 0.5)
        val commonUsers = cf.similarNodes(nodeSongEdges, 5)
        for ((similarNode, score) in commonUsers) {
            trustNetwork.addNodeToNodeEdge(NodeTrustEdgeWithSourceAndTarget(NodeTrustEdge(score), node, similarNode))
        }
    }
    trustNetwork.resetRandomWalks()
    val stringifiedTrustNetwork = Json.encodeToString(trustNetwork.serialize())
    File("$contextDir/dataset/test_network.txt").writeText(stringifiedTrustNetwork)
}

