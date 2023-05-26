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

// When a user selects a song, they want it to be a non-sybil/spam entry
// Spamming is also bad for a system's resources

fun main() {
    lateinit var trustNetwork: TrustNetwork
    val currentDir = System.getProperty("user.dir")
    val contextDir = "$currentDir/musicdao/src/test/resources"
    val loadedTrustNetwork = File("$contextDir/dataset/test_network.txt").readText()
    val subNetworks = Json.decodeFromString<SerializedSubNetworks>(loadedTrustNetwork)
    val fileOut = File("$contextDir/dataset/giga_sybil_experiment_final.txt")
    val seed = 5
    val rng = Random(seed)
    fileOut.createNewFile()
    val percentageAttackers = 0.05
    trustNetwork = TrustNetwork(subNetworks, "d7083f5e1d50c264277d624340edaaf3dc16095b")
    val dotFile = File("$contextDir/dataset/test.dot")
    dotFile.createNewFile()
    val interactionFile = File("$contextDir/dataset/kaggle_visible_evaluation_triplets.txt")
    val songListenCount = mutableMapOf<SongRecommendation, Int>()
    try {
        BufferedReader(FileReader(interactionFile)).use { br ->
            var line: String?
            while (br.readLine().also { line = it } != null) {
                val words = line?.split("\\s".toRegex())?.toTypedArray()!!
                songListenCount[SongRecommendation(words[1])] =
                    (songListenCount[SongRecommendation(words[1])] ?: 0) + words[2].toInt()
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }

    val sortedSongListenCount = songListenCount.toList()
        .sortedBy { (_, value) -> value }
        .takeLast(500)
        .reversed()
        .toMap()

    val popularSongs = sortedSongListenCount.keys.toList()

    val allNodes = trustNetwork.nodeToNodeNetwork.getAllNodes().toList()
    val totalNodes = allNodes.size
    val nAttackerNodes = (totalNodes * percentageAttackers).toInt()
    val randomAttackerIndices = generateSequence {
        rng.nextInt(0, totalNodes - 1)
    }.distinct()
        .filter { trustNetwork.nodeToNodeNetwork.graph.incomingEdgesOf(allNodes[it]).size > 0 }
        .take(nAttackerNodes)
        .sorted()
        .toSet()
    val attackerNodes = mutableListOf<Node>()
    for (i in randomAttackerIndices) {
        attackerNodes.add(allNodes[i])
    }
    for ((index, attackerNode) in attackerNodes.withIndex()) {
        val typeAttack = rng.nextInt(2)
        val sybilSongRecs = (1..3).map { SongRecommendation("sybilSong-$index-$it") }
        sybilSongRecs.forEach {
            trustNetwork.addSongRec(it)
        }
        val attackerEdges = trustNetwork.nodeToNodeNetwork.graph.outgoingEdgesOf(attackerNode).toList()
        for(i in 0 until attackerEdges.size) {
            trustNetwork.nodeToNodeNetwork.removeEdge(attackerEdges[i])
        }
        //linear attack
        when (typeAttack) {
            0 -> {
                var newNode = Node("sybilNode-$index-1")
                trustNetwork.addNode(newNode)
                //arbitrary large value
                trustNetwork.nodeToNodeNetwork.addEdge(attackerNode, newNode, NodeTrustEdge(500.0))
                for (sybilSongRec in sybilSongRecs) {
                    trustNetwork.nodeToSongNetwork.addEdge(attackerNode, sybilSongRec, NodeSongEdge(0.2))
                }
                for (i in (1..1000)) {
                    val nextNode = Node("sybilNode-$index-${i + 1}")
                    val randomPopularSongInts = generateSequence {
                        rng.nextInt(popularSongs.size)
                    }.distinct().take(5).toSet()
                    val popularSongsAttack = mutableListOf<SongRecommendation>()
                    for (j in randomPopularSongInts) {
                        popularSongsAttack.add(popularSongs[j])
                    }
                    for (sybilSongRec in sybilSongRecs) {
                        trustNetwork.nodeToSongNetwork.addEdge(newNode, sybilSongRec, NodeSongEdge(0.1))
                    }
                    for (attackSongRec in popularSongsAttack) {
                        trustNetwork.nodeToSongNetwork.addEdge(newNode, attackSongRec, NodeSongEdge(0.1))
                    }
                    if (90 != i) {
                        trustNetwork.addNode(nextNode)
                        trustNetwork.nodeToNodeNetwork.addEdge(newNode, nextNode, NodeTrustEdge(1.0))
                    }
                    newNode = nextNode
                }
            }
            // cycle attack
            else -> {
                for (sybilSongRec in sybilSongRecs) {
                    trustNetwork.nodeToSongNetwork.addEdge(attackerNode, sybilSongRec, NodeSongEdge(0.2))
                }
                for (i in (1..2)) {
                    val nextNode = Node("sybilNode-$index-$i")
                    trustNetwork.addNode(nextNode)
                    val randomPopularSongInts = generateSequence {
                        rng.nextInt(popularSongs.size)
                    }.distinct().take(5).toSet()
                    val popularSongsAttack = mutableListOf<SongRecommendation>()
                    for (j in randomPopularSongInts) {
                        popularSongsAttack.add(popularSongs[j])
                    }
                    for (sybilSongRec in sybilSongRecs) {
                        trustNetwork.nodeToSongNetwork.addEdge(nextNode, sybilSongRec, NodeSongEdge(0.1))
                    }
                    for (attackSongRec in popularSongsAttack) {
                        trustNetwork.nodeToSongNetwork.addEdge(nextNode, attackSongRec, NodeSongEdge(0.1))
                    }
                    if (5 != i) {
                        trustNetwork.nodeToNodeNetwork.addEdge(attackerNode, nextNode, NodeTrustEdge(100.0))
                        trustNetwork.nodeToNodeNetwork.addEdge(nextNode, attackerNode, NodeTrustEdge(100.0))
                    }
                }
            }
        }
    }
    val newNodeToNodeNetwork = trustNetwork.nodeToNodeNetwork
    val newNodeToSongNetwork = trustNetwork.nodeToSongNetwork
    val nRootNodes = 100
    val rootNodeIndices = generateSequence {
        rng.nextInt(0, totalNodes - 1)
    }.filterNot { randomAttackerIndices.contains(it) }
        .distinct()
        .filter { trustNetwork.nodeToNodeNetwork.graph.outgoingEdgesOf(allNodes[it]).size > 3 }
        .take(nRootNodes)
        .sorted()
        .toSet()
    val rootNodes = mutableListOf<Node>()
    for (i in rootNodeIndices) {
        rootNodes.add(allNodes[i])
    }

    val alphaDecayValues: List<Double> = (1..10 step 1).map { it.toDouble() / 10 }
    val betaDecayValues: List<Double> = (0..10 step 1).map { it.toDouble() / 10 }

    for(alphaDecay in alphaDecayValues) {
        for (betaDecay in betaDecayValues) {
            println("BETA DECAY $betaDecay experiments starting")
            var reputationGainForSybils = 0.0
            var top100SongsSybil = 0
            var top1000SongsSybil = 0
            var top2000SongsSybil = 0
            var top5000SongsSybil = 0
            for ((index, rootNode) in rootNodes.withIndex()) {
                println("ROOT NODE $index")
                var allSongs = trustNetwork.nodeToSongNetwork.getAllSongs().toList()
                for (song in allSongs) {
                    if (song.getTorrentHash().contains("sybilSong") && song.rankingScore > 0.0) {
                        reputationGainForSybils += song.rankingScore
                    }
                    song.rankingScore = 0.0
                }
                for (node in allNodes) {
                    node.rankingScore = 0.0
                }
                val nodeToNodeNetwork = NodeToNodeNetwork(newNodeToNodeNetwork.graph)
                val nodeToSongNetwork = NodeToSongNetwork(newNodeToSongNetwork.graph)
                trustNetwork = TrustNetwork(
                    SubNetworks(nodeToNodeNetwork, nodeToSongNetwork),
                    rootNode.getIpv8(),
                    alphaDecay,
                    betaDecay,
                    0.0,
                    100000
                )
                allSongs = trustNetwork.nodeToSongNetwork.getAllSongs().toList()
                println("REPUTATION GAIN $reputationGainForSybils")
                val top100Songs = allSongs.sortedBy { it.rankingScore }.takeLast(100).toList()
                for ((i, song) in top100Songs.withIndex()) {
                    if (song.getTorrentHash().contains("sybil")) {
                        top100SongsSybil += i
                    }
                }
                val top1000Songs = allSongs.sortedBy { it.rankingScore }.takeLast((1000).toInt()).toList()
                for ((i, song) in top1000Songs.withIndex()) {
                    if (song.getTorrentHash().contains("sybil")) {
                        top1000SongsSybil += i
                    }
                }
                val top2000Songs = allSongs.sortedBy { it.rankingScore }.takeLast(2000).toList()
                for ((i, song) in top2000Songs.withIndex()) {
                    if (song.getTorrentHash().contains("sybil")) {
                        top2000SongsSybil += i
                    }
                }
                val top5000Songs = allSongs.sortedBy { it.rankingScore }.takeLast(5000).toList()
                for ((i, song) in top5000Songs.withIndex()) {
                    if (song.getTorrentHash().contains("sybil")) {
                        top5000SongsSybil += i
                    }
                }
                for (song in allSongs) {
                    if (song.getTorrentHash().contains("sybilSong") && song.rankingScore > 0.0) {
                        reputationGainForSybils += song.rankingScore
                    }
                    song.rankingScore = 0.0
                }
                for (node in allNodes) {
                    node.rankingScore = 0.0
                }
            }

            fileOut.appendText("ALPHA DECAY: $alphaDecay BETA DECAY: $betaDecay REPUTATION GAIN: $reputationGainForSybils")
            fileOut.appendText("\n")
            fileOut.appendText("TOP 100 SONGS SYBIL: ${top100SongsSybil/100}")
            fileOut.appendText("\n")
            fileOut.appendText("TOP 1000 SONGS SYBIL: ${top1000SongsSybil/100}")
            fileOut.appendText("\n")
            fileOut.appendText("TOP 2000 SONGS SYBIL: ${top2000SongsSybil/100}")
            fileOut.appendText("\n")
            fileOut.appendText("TOP 5000 SONGS SYBIL: ${top5000SongsSybil/100}")
            fileOut.appendText("\n")
        }
    }
}
