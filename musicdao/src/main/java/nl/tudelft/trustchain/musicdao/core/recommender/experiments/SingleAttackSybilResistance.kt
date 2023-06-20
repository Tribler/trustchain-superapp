package nl.tudelft.trustchain.musicdao.core.recommender.experiments

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import nl.tudelft.trustchain.musicdao.core.recommender.graph.*
import nl.tudelft.trustchain.musicdao.core.recommender.model.*
import nl.tudelft.trustchain.musicdao.core.recommender.networks.SerializedSubNetworks
import nl.tudelft.trustchain.musicdao.core.recommender.networks.SubNetworks
import nl.tudelft.trustchain.musicdao.core.recommender.networks.TrustNetwork
import java.io.File
import kotlin.random.Random

fun main() {
    lateinit var trustNetwork: TrustNetwork
    val currentDir = System.getProperty("user.dir")
    val contextDir = "$currentDir/musicdao/src/test/resources"
    val loadedTrustNetwork = File("$contextDir/dataset/test_network.txt").readText()
    val subNetworks = Json.decodeFromString<SerializedSubNetworks>(loadedTrustNetwork)
    val fileOut = File("$contextDir/dataset/linear_single_attack_no_alpha_decay.txt")
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
        var newNode = Node("sybilNode-1")
        val sybilSongRecs = (1..3).map { Recommendation("sybilSong-$it") }
        nodeToNodeNetwork.addNode(newNode)
        nodeToSongNetwork.addNodeOrSong(newNode)
        nodeToNodeNetwork.addEdge(attackerNode, newNode, NodeTrustEdge(1.0))
        for (sybilSongRec in sybilSongRecs) {
            nodeToSongNetwork.addNodeOrSong(sybilSongRec)
            nodeToSongNetwork.addEdge(attackerNode, sybilSongRec, NodeSongEdge(0.2))
        }
        for (i in (1 .. sybils)) {
            val nextNode = Node("sybilNode-${i + 1}")
            for (sybilSongRec in sybilSongRecs) {
                nodeToSongNetwork.addEdge(newNode, sybilSongRec, NodeSongEdge(0.33))
            }
            nodeToNodeNetwork.addNode(nextNode)
            nodeToSongNetwork.addNodeOrSong(nextNode)
            nodeToNodeNetwork.addEdge(newNode, nextNode, NodeTrustEdge(1.0))
            newNode = nextNode
        }
        var allSongs = nodeToSongNetwork.getAllSongs().toList()
        for (song in allSongs) {
            song.rankingScore = 0.0
        }
        trustNetwork = TrustNetwork(
            SubNetworks(nodeToNodeNetwork, nodeToSongNetwork),
            rootNode.getKey(),
            0.005,
            0.8
        )
        allSongs = trustNetwork.nodeToSongNetwork.getAllSongs().toList()
        for (song in allSongs) {
            if (song.getUniqueIdentifier().contains("sybilSong") && song.rankingScore > 0.0) {
                reputationGainForSybils += song.rankingScore
            }
        }

            fileOut.appendText("Sybils: $sybils")
            fileOut.appendText("\n")
            fileOut.appendText("REPUTATION GAIN: $reputationGainForSybils")
            fileOut.appendText("\n")
        }
    }
