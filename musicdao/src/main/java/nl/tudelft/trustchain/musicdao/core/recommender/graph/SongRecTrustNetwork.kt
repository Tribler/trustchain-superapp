package nl.tudelft.trustchain.musicdao.core.recommender.graph

import mu.KotlinLogging
import nl.tudelft.trustchain.musicdao.core.recommender.model.*

class SongRecTrustNetwork: TrustNetwork {

    private val logger = KotlinLogging.logger {}
    private var songRecListenCount: MutableMap<SongRecommendation, Int> = mutableMapOf()
    private var songRecommenders: MutableMap<SongRecommendation, List<Node>> = mutableMapOf()
    private val recommendersPerSong = 5

    constructor(sourceNodeAddress: String): super(sourceNodeAddress)

    constructor(subNetworks: SubNetworks, sourceNodeAddress: String, songCountList: MutableMap<SongRecommendation, Int>, songRecommendersStored: MutableMap<SongRecommendation, List<Node>>): super(subNetworks, sourceNodeAddress) {
        songRecListenCount = songCountList
        songRecommenders = songRecommendersStored
    }

    fun changeSongRecListenCount(songRec: SongRecommendation, newCount: Int): Boolean {
        if(songRec in songRecListenCount && songRecListenCount[songRec] == newCount || newCount < 0)
            return false
        songRecListenCount[songRec] = newCount
        val newTotalCount = songRecListenCount.values.sum()
        val nodeToSongEdges = mutableListOf<NodeSongEdgeWithNodeAndSongRec>()
        for((rec, count) in songRecListenCount) {
            val affinity = count.toDouble() / newTotalCount
            nodeToSongEdges.add(NodeSongEdgeWithNodeAndSongRec(NodeSongEdge(affinity), rootNode, rec))
        }
        return bulkAddNodeToSongEdgesForNode(nodeToSongEdges, rootNode)
    }

    fun setNewSongListenCount(songRecAndCount: MutableMap<SongRecommendation, Int>): Boolean {
        songRecListenCount = songRecAndCount
        val newTotalCount = songRecListenCount.values.sum()
        val nodeToSongEdges = mutableListOf<NodeSongEdgeWithNodeAndSongRec>()
        for((rec, count) in songRecListenCount) {
            val affinity = count.toDouble() / newTotalCount
            nodeToSongEdges.add(NodeSongEdgeWithNodeAndSongRec(NodeSongEdge(affinity), rootNode, rec))
        }
        return bulkAddNodeToSongEdgesForNode(nodeToSongEdges, rootNode)
    }
    override fun bulkAddNodeToSongEdgesForNode(edges: List<NodeSongEdgeWithNodeAndSongRec>, sourceNode: Node): Boolean {
        var edgeAdditionFailure = false
        if(edges.any { it.node != sourceNode })
            return false
        for(edge in edges) {
            var existingAffinity = 0.0
            var isNewEdge = true
            if (containsEdge(edge.node, edge.songRec)) {
                val existingEdgeTimestamp = nodeToSongNetwork.graph.getEdge(edge.node, edge.songRec).timestamp
                existingAffinity = nodeToSongNetwork.graph.getEdge(edge.node, edge.songRec).affinity
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
            if (!containsSongRec(edge.songRec)) {
                if (!addSongRec(edge.songRec)) {
                    return false
                }
            }
            nodeToSongNetwork.addEdge(edge.node, edge.songRec, edge.nodeSongEdge).also {
                if(!it) {
                    logger.error { "Couldn't add edge from ${edge.node} to ${edge.songRec}" }
                    edgeAdditionFailure = true
                } else {
                    if(edge.node == rootNode) {
                        var affinityDelta = edge.nodeSongEdge.affinity - existingAffinity
                        updateNodeTrust(edge.songRec, affinityDelta, isNewEdge)
                    }
                }
            }
        }
        incrementalPersonalizedPageRank.modifyEdges(setOf(sourceNode))
        incrementalHybridPersonalizedPageRankSalsa.modifyNodesOrSongs(setOf(sourceNode), nodeToNodeNetwork.getAllNodes().toList())
        return edgeAdditionFailure
    }

    fun updateNodeTrust(songRec: SongRecommendation, affinityDelta: Double, isNewEdge: Boolean) {
        val rootNeighborEdges = nodeToNodeNetwork.graph.outgoingEdgesOf(rootNode)
        if(isNewEdge) {
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
