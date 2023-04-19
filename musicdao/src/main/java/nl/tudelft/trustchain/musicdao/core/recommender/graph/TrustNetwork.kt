package nl.tudelft.trustchain.musicdao.core.recommender.graph

import mu.KotlinLogging
import nl.tudelft.trustchain.musicdao.core.recommender.model.*
import nl.tudelft.trustchain.musicdao.core.recommender.ranking.IncrementalHybridPersonalizedPageRankSalsa
import nl.tudelft.trustchain.musicdao.core.recommender.ranking.IncrementalPersonalizedPageRank
import java.sql.Timestamp

class TrustNetwork(serializedGraphs: SerializedGraphs, sourceNodeAddress: String) {
    private val nodeToNodeNetwork: NodeToNodeNetwork
    private val nodeToSongNetwork: NodeToSongNetwork
    private val incrementalPersonalizedPageRank: IncrementalPersonalizedPageRank
    private val incrementalHybridPersonalizedPageRankSalsa: IncrementalHybridPersonalizedPageRankSalsa
    private val allNodes: MutableList<Node>
    private val logger = KotlinLogging.logger {}
    val rootNode: Node

    companion object {
        const val MAX_WALK_LENGTH = 1000
        const val REPETITIONS = 10000
        const val RESET_PROBABILITY = 0.01f

    }

    init {
        nodeToNodeNetwork = NodeToNodeNetwork(serializedGraphs.nodeToNodeNetwork)
        nodeToSongNetwork = NodeToSongNetwork(serializedGraphs.nodeToSongNetwork)
        val allNodesList = nodeToNodeNetwork.getAllNodes()
        allNodes = allNodesList.toMutableList()
        rootNode = allNodes.first { it.getIpv8() == sourceNodeAddress}
        incrementalPersonalizedPageRank = IncrementalPersonalizedPageRank(MAX_WALK_LENGTH, REPETITIONS, rootNode, RESET_PROBABILITY, nodeToNodeNetwork.graph)
        incrementalHybridPersonalizedPageRankSalsa = IncrementalHybridPersonalizedPageRankSalsa(MAX_WALK_LENGTH, REPETITIONS, rootNode, RESET_PROBABILITY, nodeToSongNetwork.graph)
    }
    private fun getEdgeTimestamp(source: Node, target: NodeOrSong): Timestamp {
        TODO("Not yet implemented")
    }

    fun addNodeToSongEdge(edge: NodeSongEdgeWithNodeAndSongRec): Boolean {
        var existingAffinity = 0.0
        if(containsEdge(edge.node, edge.songRec)) {
            val existingEdgeTimestamp = nodeToSongNetwork.graph.getEdge(edge.node, edge.songRec).timestamp
            existingAffinity = nodeToSongNetwork.graph.getEdge(edge.node, edge.songRec).affinity
            if(existingEdgeTimestamp >= edge.nodeSongEdge.timestamp) {
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
        return nodeToSongNetwork.addEdge(edge.node, edge.songRec, edge.nodeSongEdge).also {
            if(!it) {
                logger.error { "Couldn't add edge from ${edge.node} to ${edge.songRec}" }
            } else {
                if(edge.node == rootNode) {
                    updateNodeTrust(edge.songRec, edge.nodeSongEdge.affinity - existingAffinity)
                }
                incrementalPersonalizedPageRank.modifyEdges(setOf(rootNode))
                incrementalHybridPersonalizedPageRankSalsa.modifyNodesOrSongs(setOf(rootNode), setOf(edge.songRec))
            }
        }
    }

    private fun updateNodeTrust(songRec: SongRecommendation, affinityDelta: Double) {
        val recommenderNodeEdges = nodeToSongNetwork.graph.outgoingEdgesOf(songRec)
        val rootNeighborEdges = nodeToNodeNetwork.graph.outgoingEdgesOf(rootNode)
        for(recommenderNodeEdge in recommenderNodeEdges) {
            val trustDelta = recommenderNodeEdge.affinity * affinityDelta
            val recommenderNode = nodeToSongNetwork.graph.getEdgeTarget(recommenderNodeEdge) as Node
            val existingTrustEdgeToNode = rootNeighborEdges.find { nodeToNodeNetwork.graph.getEdgeTarget(it) == recommenderNode }
            val existingTrust = existingTrustEdgeToNode?.trust ?: 0.0
            val newTrust = existingTrust + trustDelta
            addNodeToNodeEdge(NodeTrustEdgeWithSourceAndTarget(NodeTrustEdge(newTrust), rootNode, recommenderNode))
        }
    }

    fun addNodeToNodeEdge(edge: NodeTrustEdgeWithSourceAndTarget): Boolean {
        if(containsEdge(edge.sourceNode, edge.targetNode)) {
            val existingEdgeTimestamp = nodeToNodeNetwork.graph.getEdge(edge.sourceNode, edge.targetNode).timestamp
            if(existingEdgeTimestamp >= edge.nodeTrustEdge.timestamp) {
                return false
            }
        }
        if (!containsNode(edge.sourceNode)) {
            if (!addNode(edge.sourceNode)) {
                return false
            }
        }
        if (!containsNode(edge.targetNode)) {
            if (!addNode(edge.targetNode)) {
                return false
            }
        }
        return nodeToNodeNetwork.addEdge(edge.sourceNode, edge.targetNode, edge.nodeTrustEdge)
    }

    private fun containsEdge(source: Node, target: NodeOrSong): Boolean {
        return if(target is SongRecommendation) nodeToSongNetwork.graph.containsEdge(source, target) else nodeToNodeNetwork.graph.containsEdge(source, target as Node)
    }

    private fun containsNode(node: Node): Boolean {
        return nodeToSongNetwork.graph.containsVertex(node) && nodeToSongNetwork.graph.containsVertex(node)
    }

    private fun containsSongRec(songRec: SongRecommendation): Boolean {
        return nodeToSongNetwork.graph.containsVertex(songRec)
    }

    fun addNode(node: Node): Boolean {
        return nodeToSongNetwork.addNodeOrSong(node) && nodeToNodeNetwork.addNode(node)
    }

    fun addSongRec(songRec: SongRecommendation): Boolean {
        return nodeToSongNetwork.addNodeOrSong(songRec)
    }


}
