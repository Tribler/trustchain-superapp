package nl.tudelft.trustchain.musicdao.core.recommender.experiments

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import nl.tudelft.trustchain.musicdao.core.recommender.model.*
import nl.tudelft.trustchain.musicdao.core.recommender.networks.SerializedSubNetworks
import nl.tudelft.trustchain.musicdao.core.recommender.networks.TrustNetwork
import java.io.File
import kotlin.random.Random
import kotlin.math.pow


fun rboHelper(ret: Double, i: Int, d: Int, list1: List<Recommendation>, list2: List<Recommendation>, p: Double): Double {
    val l1 = list1.take(i).toSet()
    val l2 = list2.take(i).toSet()
    val aD = (l1.intersect(l2).size).toDouble() / i
    val term = p.pow(i) * aD
    return if(d == i)
        ret + term
    else
        rboHelper(ret + term, i +1, d, list1, list2, p)
}
fun rbo(list1: List<Recommendation>, list2: List<Recommendation>, p: Double = 0.8): Double {
    val k = maxOf(list1.size, list2.size)
    val xK = list1.toSet().intersect(list2.toSet()).size.toDouble()
    val sum = rboHelper(0.0, 1, k, list1, list2, p)
    return ((xK/k) * p.pow(k)) + (((1.0 - p) / p) * sum)
}

fun main() {
    lateinit var trustNetwork: TrustNetwork
    val currentDir = System.getProperty("user.dir")
    val contextDir = "$currentDir/musicdao/src/test/resources"
    val loadedTrustNetwork = File("$contextDir/dataset/test_network.txt").readText()
    val subNetworks = Json.decodeFromString<SerializedSubNetworks>(loadedTrustNetwork)
    val fileOut = File("$contextDir/dataset/relevance_experiment_out_final.txt")
    val seed = 5
    val rng = Random(seed)
    trustNetwork = TrustNetwork(subNetworks, "d7083f5e1d50c264277d624340edaaf3dc16095b")
    val allNodes = trustNetwork.nodeToNodeNetwork.getAllNodes().toList()
    val totalNodes = allNodes.size
    fileOut.createNewFile()
    val nRootNodes = 100
    val rootNodeIndices = generateSequence {
        rng.nextInt(0, totalNodes - 1)
    }
        .distinct()
        .take(nRootNodes)
        .sorted()
        .toSet()
    val rootNodes = mutableListOf<Node>()
    for (i in rootNodeIndices) {
        rootNodes.add(allNodes[i])
    }

    val top100ListBases = mutableMapOf<Node, List<Recommendation>>()

    val decayValues: List<Double> = (0..10 step 1).map { it.toDouble() / 10 }
    val alphaDecayValues: List<Double> = (1..10 step 1).map { it.toDouble() / 10 }
    for(alphaDecay in alphaDecayValues) {
        for (betaDecay in decayValues) {
            var top100SongsRbo = 0.0
            println("$alphaDecay BETA DECAY $betaDecay experiments starting")
            var allSongs = trustNetwork.nodeToSongNetwork.getAllSongs().toList()
            for (song in allSongs) {
                song.rankingScore = 0.0
            }
            for ((index, rootNode) in rootNodes.withIndex()) {
                println("ROOT NODE $index")
                trustNetwork = TrustNetwork(
                    subNetworks,
                    rootNode.getKey(),
                    alphaDecay,
                    betaDecay
                )
                allSongs = trustNetwork.nodeToSongNetwork.getAllSongs().toList()
                val top100Songs = allSongs.sortedBy { it.rankingScore }.takeLast(100).toList().reversed()
                if (betaDecay == 0.0) {
                    top100ListBases[rootNode] = top100Songs
                }
                top100SongsRbo += rbo(top100ListBases[rootNode]!!, top100Songs)
            }
                fileOut.appendText("ALPHA DECAY: $alphaDecay BETA DECAY: $betaDecay")
                fileOut.appendText("\n")
                fileOut.appendText("TOP 100 SONGS RBO: ${top100SongsRbo / 100}")
                fileOut.appendText("\n")
            }
    }
}
