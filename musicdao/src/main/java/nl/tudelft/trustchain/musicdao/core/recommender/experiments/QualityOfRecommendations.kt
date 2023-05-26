package nl.tudelft.trustchain.musicdao.core.recommender.experiments

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import nl.tudelft.trustchain.musicdao.core.recommender.graph.*
import nl.tudelft.trustchain.musicdao.core.recommender.model.*
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import kotlin.random.Random

fun main() {
    lateinit var trustNetwork: TrustNetworkTwitter
    val currentDir = System.getProperty("user.dir")
    val contextDir = "$currentDir/musicdao/src/test/resources"
    val loadedTrustNetwork = File("$contextDir/dataset/test_network.txt").readText()
    val subNetworks = Json.decodeFromString<SerializedSubNetworks>(loadedTrustNetwork)
    val fileOut = File("$contextDir/dataset/recommendations_quality.txt")
    val seed = 5
    val rng = Random(seed)
    fileOut.createNewFile()
    trustNetwork = TrustNetworkTwitter(subNetworks, "d7083f5e1d50c264277d624340edaaf3dc16095b")

    val allNodes = trustNetwork.nodeToNodeNetwork.getAllNodes().toList()
    val totalNodes = allNodes.size
    val nRootNodes = 100
    val rootNodeIndices = generateSequence {
        rng.nextInt(0, totalNodes - 1)
    }
        .distinct()
        .filter { trustNetwork.nodeToNodeNetwork.graph.outgoingEdgesOf(allNodes[it]).size > 1 &&  trustNetwork.nodeToSongNetwork.graph.outgoingEdgesOf(allNodes[it]).size > 1 }
        .take(nRootNodes)
        .sorted()
        .toSet()
    val rootNodes = mutableListOf<Node>()
    for (i in rootNodeIndices) {
        rootNodes.add(allNodes[i])
    }


    for(alphaDecay in listOf(0.001, 0.01, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0)) {
                println("Experiments starting: Twitter")
                var top100SongsCount = 0.0
                var top500SongsCount = 0.0
                var top1000SongsCount = 0.0
                var top10000SongsCount = 0.0
                var top100000SongsCount = 0.0
                var missingSongScore = 0.0
                for ((index, rootNode) in rootNodes.withIndex()) {
                    println("ROOT NODE $index")
                    val nodeToSongNetwork = NodeToSongNetwork(subNetworks.nodeToSongNetworkSerialized)
                    val outgoingRootNodeEdges = nodeToSongNetwork.graph.outgoingEdgesOf(rootNode)
                    val edgeWithLargestWeight = outgoingRootNodeEdges.maxBy { it.affinity }
                    val songWithLargestAffinity = nodeToSongNetwork.graph.getEdgeTarget(edgeWithLargestWeight)
                    nodeToSongNetwork.removeEdge(edgeWithLargestWeight)
                    var allSongs = nodeToSongNetwork.getAllSongs().toList()
                    for (song in allSongs) {
                        song.rankingScore = 0.0
                    }
                    trustNetwork = TrustNetworkTwitter(
                        SubNetworks(NodeToNodeNetwork(subNetworks.nodeToNodeNetworkSerialized), nodeToSongNetwork),
                        rootNode.getIpv8(),
                        alphaDecay,
                        0.0,
                        0.0
                    )
                    allSongs = trustNetwork.nodeToSongNetwork.getAllSongs().toList()
                    val top100Songs = allSongs.sortedBy { it.rankingScore }.takeLast((100).toInt()).toList()
                    for (song in top100Songs) {
                        if(song == songWithLargestAffinity) {
                            top100SongsCount += 1.0
                        }
                    }
                    val top500Songs = allSongs.sortedBy { it.rankingScore }.takeLast((500).toInt()).toList()
                    for (song in top500Songs) {
                        if(song == songWithLargestAffinity) {
                            top500SongsCount += 1.0
                        }
                    }
                    val top1000Songs = allSongs.sortedBy { it.rankingScore }.takeLast((1000).toInt()).toList()
                    for (song in top1000Songs) {
                        if(song == songWithLargestAffinity) {
                            top1000SongsCount += 1.0
                        }
                    }
                    val top10000Songs = allSongs.sortedBy { it.rankingScore }.takeLast((10000).toInt()).toList()
                    for (song in top10000Songs) {
                        if(song == songWithLargestAffinity) {
                            top10000SongsCount += 1.0
                        }
                    }
                    val top100000Songs = allSongs.sortedBy { it.rankingScore }.takeLast((100000).toInt()).toList()
                    for (song in top100000Songs) {
                        if(song == songWithLargestAffinity) {
                            top100000SongsCount += 1.0
                        }
                    }
                    missingSongScore += songWithLargestAffinity.rankingScore
                }

                fileOut.appendText("ALPHA DECAY: $alphaDecay")
                fileOut.appendText("\n")
                fileOut.appendText("AVERAGE SONGS SCORE: ${missingSongScore/rootNodeIndices.size}")
                fileOut.appendText("\n")
                fileOut.appendText("TOP 100 SONGS: ${top100SongsCount/rootNodeIndices.size}")
                fileOut.appendText("\n")
                fileOut.appendText("TOP 500 SONGS: ${top500SongsCount/rootNodeIndices.size}")
                fileOut.appendText("\n")
                fileOut.appendText("TOP 1000 SONGS: ${top1000SongsCount/rootNodeIndices.size}")
                fileOut.appendText("\n")
                fileOut.appendText("TOP 10000 SONGS: ${top10000SongsCount/rootNodeIndices.size}")
                fileOut.appendText("\n")
                fileOut.appendText("TOP 100000 SONGS: ${top100000SongsCount/rootNodeIndices.size}")
                fileOut.appendText("\n")
        }
    }
