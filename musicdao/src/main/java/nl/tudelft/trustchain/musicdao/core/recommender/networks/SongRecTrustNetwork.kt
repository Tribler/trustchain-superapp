package nl.tudelft.trustchain.musicdao.core.recommender.networks

import android.annotation.SuppressLint
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import nl.tudelft.trustchain.musicdao.core.recommender.model.*
import java.io.File
import java.nio.file.Files

lateinit var trustNetworkInstance: SongRecTrustNetwork
class SongRecTrustNetwork: TrustNetwork {

    private val logger = KotlinLogging.logger {}
    private var songRecListenCount: MutableMap<Recommendation, Int> = mutableMapOf()
    private var songRecommenders: MutableMap<Recommendation, List<Node>> = mutableMapOf()
    private val recommendersPerSong = 5
    private var appDir = ""

    constructor(sourceNodeAddress: String): super(sourceNodeAddress)

    constructor(sourceNodeAddress: String, appDirectory: String): super(sourceNodeAddress, appDirectory) {
        appDir = appDirectory
        songRecommenders = fetchSongRecommendersStored(appDirectory) ?: mutableMapOf()
        songRecListenCount = fetchSongCountList(appDirectory) ?: mutableMapOf()
    }

    companion object {
        private const val songCountListenPath = "/trusted_recommender/" + "songCountListen.txt"
        private const val songRecommendersPath = "/trusted_recommender/" + "songRecommenders.txt"
        private fun fetchSongCountList(appDirectory: String): MutableMap<Recommendation, Int>? {
            val songCountListFilePath = File(appDirectory + songCountListenPath)
            return if (songCountListFilePath.exists()) {
                val songCountPathText = songCountListFilePath.readText()
                val songCountListInString = Json.decodeFromString<Map<String, Int>>(songCountPathText)
                return songCountListInString.mapKeys { Recommendation(it.key) }.toMutableMap()
            } else {
                null
            }
        }

        private fun fetchSongRecommendersStored(appDirectory: String): MutableMap<Recommendation, List<Node>>? {
            val songRecommendersFilePath = File(appDirectory + songRecommendersPath)
            return if (songRecommendersFilePath.exists()) {
                val songRecommendersText = songRecommendersFilePath.readText()
                val songCountListInString = Json.decodeFromString<Map<String, List<String>>>(songRecommendersText)
                return songCountListInString.mapKeys { Recommendation(it.key) }.mapValues { listOfNodes -> listOfNodes.value.map { Node(it) } }.toMutableMap()
            } else {
                null
            }
        }
        fun getInstance(
            sourceNodeAddress: String, appDirectory: String
        ): SongRecTrustNetwork {
            if (!::trustNetworkInstance.isInitialized) {
                trustNetworkInstance = SongRecTrustNetwork(sourceNodeAddress, appDirectory)
            }
            return trustNetworkInstance
        }
    }

    @SuppressLint("NewApi")
    fun overwriteLocalFiles() {
        val songCountListFile = File("$appDir$songCountListenPath")
        Files.deleteIfExists(songCountListFile.toPath())
        val stringifiedListenCount = Json.encodeToString(songRecListenCount.mapKeys { it.key.getUniqueIdentifier() } )
        songCountListFile.parentFile?.mkdirs()
        songCountListFile.createNewFile()
        songCountListFile.writeText(stringifiedListenCount)


        val songRecommendersFile = File("$appDir$songRecommendersPath")
        Files.deleteIfExists(songRecommendersFile.toPath())
        val stringifiedRecommenders = Json.encodeToString(songRecommenders.mapKeys { it.key.getUniqueIdentifier() }.mapValues { nodeList -> nodeList.value.map { it.getKey() } } )
        songCountListFile.parentFile?.mkdirs()
        songRecommendersFile.createNewFile()
        songRecommendersFile.writeText(stringifiedRecommenders)
    }

    fun incrementSongRecListenCount(songRec: Recommendation): Boolean {
        return if (songRec in songRecListenCount)
            changeSongRecListenCount(songRec, songRecListenCount[songRec]!! + 1)
        else
            changeSongRecListenCount(songRec, 1)
    }

    fun changeSongRecListenCount(songRec: Recommendation, newCount: Int): Boolean {
        if(songRec in songRecListenCount && songRecListenCount[songRec] == newCount || newCount < 0)
            return false
        songRecListenCount[songRec] = newCount
        val newTotalCount = songRecListenCount.values.sum()
        val nodeToSongEdges = mutableListOf<NodeRecEdge>()
        for((rec, count) in songRecListenCount) {
            val affinity = count.toDouble() / newTotalCount
            nodeToSongEdges.add(NodeRecEdge(NodeSongEdge(affinity), rootNode, rec))
        }
        return bulkAddNodeToSongEdgesForNode(nodeToSongEdges, rootNode).also {
            overwriteSaveFiles()
            overwriteLocalFiles()
        }
    }

    fun setNewSongListenCount(songRecAndCount: MutableMap<Recommendation, Int>): Boolean {
        songRecListenCount = songRecAndCount
        val newTotalCount = songRecListenCount.values.sum()
        val nodeToSongEdges = mutableListOf<NodeRecEdge>()
        for((rec, count) in songRecListenCount) {
            val affinity = count.toDouble() / newTotalCount
            nodeToSongEdges.add(NodeRecEdge(NodeSongEdge(affinity), rootNode, rec))
        }
        return bulkAddNodeToSongEdgesForNode(nodeToSongEdges, rootNode).also {
            overwriteSaveFiles()
            overwriteLocalFiles()
        }
    }

    fun refreshRankings() {
        incrementalHybridPersonalizedPageRankSalsa.calculateRankings()
    }
    override fun bulkAddNodeToSongEdgesForNode(edges: List<NodeRecEdge>, sourceNode: Node): Boolean {
        var edgeAdditionFailure = false
        if(edges.any { it.node != sourceNode })
            return false
        for(edge in edges) {
            var existingAffinity = 0.0
            var isNewEdge = true
            if (containsEdge(edge.node, edge.rec)) {
                val existingEdgeTimestamp = nodeToSongNetwork.graph.getEdge(edge.node, edge.rec).timestamp
                existingAffinity = nodeToSongNetwork.graph.getEdge(edge.node, edge.rec).affinity
                isNewEdge = false
                if (existingEdgeTimestamp >= edge.nodeSongEdge.timestamp) {
                    return false
                }
            }
            if (!containsNode(edge.node)) {
                if (!addNode(edge.node)) {
                    return false
                }
            }
            if (!containsSongRec(edge.rec)) {
                if (!addSongRec(edge.rec)) {
                    return false
                }
            }
            nodeToSongNetwork.addEdge(edge.node, edge.rec, edge.nodeSongEdge).also {
                if(!it) {
                    logger.error { "Couldn't add edge from ${edge.node} to ${edge.rec}" }
                    edgeAdditionFailure = true
                } else {
                    if(edge.node == rootNode) {
                        val affinityDelta = edge.nodeSongEdge.affinity - existingAffinity
                        updateNodeTrust(edge.rec, affinityDelta, isNewEdge)
                    }
                }
            }
        }
        incrementalPersonalizedPageRank.modifyEdges(setOf(sourceNode))
        incrementalHybridPersonalizedPageRankSalsa.modifyNodesOrSongs(setOf(sourceNode), nodeToNodeNetwork.getAllNodes().toList())
        return edgeAdditionFailure
    }

    fun updateNodeTrust(songRec: Recommendation, affinityDelta: Double, isNewEdge: Boolean) {
        val rootNeighborEdges = nodeToNodeNetwork.graph.outgoingEdgesOf(rootNode)
        val fetchNewRecommenders = (isNewEdge && (!songRecommenders.containsKey(songRec) || songRecommenders[songRec]!!.isEmpty()))
        if(fetchNewRecommenders) {
            val hybridRandomWalks = incrementalHybridPersonalizedPageRankSalsa.randomWalks
            val recommendationCount = mutableMapOf<Node, Int>()
            for(walk in hybridRandomWalks) {
                if(walk.contains(songRec)) {
                    for(node in walk) {
                        if(node is Node) {
                            recommendationCount[node] = (recommendationCount[node] ?: 0) + 1
                        } else if(node == songRec) {
                            break
                        }
                    }
                }
            }
            val topSongRecommenders = recommendationCount.toList().sortedBy { it.second }.takeLast(recommendersPerSong).map { it.first }
            songRecommenders[songRec] = topSongRecommenders
            for(recommender in topSongRecommenders) {
                val existingTrustEdgeToNode =
                    rootNeighborEdges.find { nodeToNodeNetwork.graph.getEdgeTarget(it) == recommender }
                val existingTrust = existingTrustEdgeToNode?.trust ?: 0.0
                addNodeToNodeEdge(NodeTrustEdgeWithSourceAndTarget(NodeTrustEdge(existingTrust + affinityDelta), rootNode, recommender))
            }
        } else {
            val pastSongRecommenders = songRecommenders[songRec]!!
            for(recommender in pastSongRecommenders) {
                val existingTrustEdgeToNode =
                    rootNeighborEdges.find { nodeToNodeNetwork.graph.getEdgeTarget(it) == recommender }
                val existingTrust = existingTrustEdgeToNode?.trust ?: 0.0
                if(existingTrustEdgeToNode != null && existingTrust + affinityDelta < 0.0) {
                    nodeToNodeNetwork.removeEdge(existingTrustEdgeToNode)
                } else {
                    addNodeToNodeEdge(NodeTrustEdgeWithSourceAndTarget(NodeTrustEdge(if(existingTrust + affinityDelta < 0) 0.0 else existingTrust + affinityDelta), rootNode, recommender))
                }
            }
        }
    }

}
