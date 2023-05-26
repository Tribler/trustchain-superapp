package nl.tudelft.trustchain.musicdao.core.recommender.experiments

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import nl.tudelft.trustchain.musicdao.core.recommender.graph.*
import nl.tudelft.trustchain.musicdao.core.recommender.model.*
import java.io.File
import kotlin.random.Random

fun main() {
    lateinit var trustNetwork: TrustNetwork
    val currentDir = System.getProperty("user.dir")
    val contextDir = "$currentDir/musicdao/src/test/resources"
    val loadedTrustNetwork = File("$contextDir/dataset/test_network.txt").readText()
    val subNetworks = Json.decodeFromString<SerializedSubNetworks>(loadedTrustNetwork)
    val fileOut = File("$contextDir/dataset/parallel_single_attack.txt")
    val seed = 5
    val rng = Random(seed)
    fileOut.createNewFile()
    trustNetwork = TrustNetwork(subNetworks, "d7083f5e1d50c264277d624340edaaf3dc16095b")

    val allNodes = trustNetwork.nodeToNodeNetwork.getAllNodes().toList()
    val totalNodes = allNodes.size
    val rootNodeIndex = generateSequence {
        rng.nextInt(0, totalNodes - 1)
    }
        .distinct()
        .filter { trustNetwork.nodeToNodeNetwork.graph.outgoingEdgesOf(allNodes[it]).size > 1 &&  trustNetwork.nodeToSongNetwork.graph.outgoingEdgesOf(allNodes[it]).size > 1 }
        .take(1)
        .sorted()
        .toSet()
        .first()

    val rootNode = allNodes[rootNodeIndex]
    val rootNodeNeighbors = trustNetwork.nodeToNodeNetwork.graph.outgoingEdgesOf(rootNode).toList()
    val neighborsSize = rootNodeNeighbors.size
    val attackerNodeIndex = rng.nextInt(0, neighborsSize - 1)
    val attackerNodeEdge = rootNodeNeighbors[attackerNodeIndex]
    val attackerNode = trustNetwork.nodeToNodeNetwork.graph.getEdgeTarget(attackerNodeEdge)
    val attackerNodeNeighborEdges = trustNetwork.nodeToNodeNetwork.graph.outgoingEdgesOf(attackerNode).toList()
    for(i in (0 until attackerNodeNeighborEdges.size)) {
        trustNetwork.nodeToNodeNetwork.removeEdge(attackerNodeNeighborEdges[i])
    }
    val allSybilAttacks: List<Int> = (1..100 step 1).map { it }
    for(sybils in allSybilAttacks) {
        var reputationGainForSybils = 0.0
        println("$sybils")
        val nodeToSongNetwork = NodeToSongNetwork(subNetworks.nodeToSongNetworkSerialized)
        val nodeToNodeNetwork = NodeToNodeNetwork(subNetworks.nodeToNodeNetworkSerialized)
        for(i in (0 until attackerNodeNeighborEdges.size)) {
            nodeToNodeNetwork.removeEdge(attackerNodeNeighborEdges[i])
        }
        val sybilSongRecs = (1..3).map { SongRecommendation("sybilSong-$it") }
        for (sybilSongRec in sybilSongRecs) {
            nodeToSongNetwork.addNodeOrSong(sybilSongRec)
            nodeToSongNetwork.addEdge(attackerNode, sybilSongRec, NodeSongEdge(0.2))
        }
        for (i in (1..sybils)) {
            val nextNode = Node("sybilNode-${i}")
            nodeToNodeNetwork.addNode(nextNode)
            nodeToSongNetwork.addNodeOrSong(nextNode)
            for (sybilSongRec in sybilSongRecs) {
                nodeToSongNetwork.addEdge(nextNode, sybilSongRec, NodeSongEdge(0.33))
            }
                nodeToNodeNetwork.addEdge(attackerNode, nextNode, NodeTrustEdge(1.0))
                nodeToNodeNetwork.addEdge(nextNode, attackerNode, NodeTrustEdge(1.0))
        }
        var allSongs = nodeToSongNetwork.getAllSongs().toList()
        for (song in allSongs) {
            song.rankingScore = 0.0
        }
        trustNetwork = TrustNetwork(
            SubNetworks(nodeToNodeNetwork, nodeToSongNetwork),
            rootNode.getIpv8(),
            0.1,
            0.8,
            0.0
        )
        allSongs = trustNetwork.nodeToSongNetwork.getAllSongs().toList()
        for (song in allSongs) {
            if (song.getTorrentHash().contains("sybilSong") && song.rankingScore > 0.0) {
                reputationGainForSybils += song.rankingScore
            }
        }
            fileOut.appendText("Sybils: $sybils")
            fileOut.appendText("\n")
            fileOut.appendText("REPUTATION GAIN: $reputationGainForSybils")
            fileOut.appendText("\n")
        }
    }
