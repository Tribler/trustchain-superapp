package nl.tudelft.trustchain.musicdao.core.recommender.graph

import mu.KotlinLogging
import nl.tudelft.trustchain.musicdao.core.recommender.model.*
import nl.tudelft.trustchain.musicdao.core.recommender.ranking.*

open class TrustNetwork {
    val nodeToNodeNetwork: NodeToNodeNetwork
    val nodeToSongNetwork: NodeToSongNetwork
    protected val incrementalPersonalizedPageRank: IncrementalPersonalizedPageRankMeritRank
    protected val incrementalHybridPersonalizedPageRankSalsa: IncrementalHybridPersonalizedPageRankSalsaMeritRank2
    private val allNodes: MutableList<Node>
    private val logger = KotlinLogging.logger {}
    val rootNode: Node
    companion object {
        const val MAX_WALK_LENGTH = 10000
        const val ALPHA_REPETITIONS = 10000
        const val BETA_REPETITIONS = 100000
        const val ALPHA_DECAY = 0.1
        const val BETA_DECAY = 0.05
        const val BETA_DECAY_THRESHOLD = 0.99
        const val EXPLORATION_PROBABILITY = 0.05

        fun deserialize(networks: SerializedSubNetworks): SubNetworks {
            return SubNetworks(NodeToNodeNetwork(networks.nodeToNodeNetworkSerialized), NodeToSongNetwork(networks.nodeToSongNetworkSerialized))
        }
    }

    constructor(subNetworks: SubNetworks, sourceNodeAddress: String, alphaDecay: Double = ALPHA_DECAY, betaDecay: Double = BETA_DECAY, explorationProbability: Double = EXPLORATION_PROBABILITY, totalRandomWalks: Int = MAX_WALK_LENGTH) {
        nodeToNodeNetwork = subNetworks.nodeToNodeNetwork
        nodeToSongNetwork = subNetworks.nodeToSongNetwork
        val allNodesList = nodeToNodeNetwork.getAllNodes()
        allNodes = allNodesList.toMutableList()
        rootNode = allNodes.first { it.getIpv8() == sourceNodeAddress}
        if(explorationProbability != 0.0) {
            incrementalPersonalizedPageRank = IncrementalPersonalizedPageRankMeritRank(MAX_WALK_LENGTH, ALPHA_REPETITIONS, rootNode, 0.005, betaDecay, 0.95, nodeToNodeNetwork.graph, false)
            incrementalPersonalizedPageRank.calculateRankings()
        } else {
            incrementalPersonalizedPageRank = IncrementalPersonalizedPageRankMeritRank(1, 1, rootNode, alphaDecay, betaDecay, 0.95, nodeToNodeNetwork.graph, false)
        }
        incrementalHybridPersonalizedPageRankSalsa = IncrementalHybridPersonalizedPageRankSalsaMeritRank2(MAX_WALK_LENGTH, BETA_REPETITIONS, rootNode, 0.001, betaDecay, 0.95,  explorationProbability, nodeToSongNetwork.graph, nodeToNodeNetwork.graph, false)
        incrementalHybridPersonalizedPageRankSalsa.calculateRankings()
    }

    constructor(serializedSubNetworks: SerializedSubNetworks, sourceNodeAddress: String, alphaDecay: Double = ALPHA_DECAY, betaDecay: Double = BETA_DECAY, explorationProbability: Double = EXPLORATION_PROBABILITY, totalRandomWalks: Int = MAX_WALK_LENGTH): this(deserialize(serializedSubNetworks), sourceNodeAddress, alphaDecay, betaDecay, explorationProbability, totalRandomWalks)

    constructor(sourceNodeAddress: String) {
        nodeToNodeNetwork = NodeToNodeNetwork()
        rootNode = Node(sourceNodeAddress)
        nodeToNodeNetwork.addNode(rootNode)
        allNodes = mutableListOf(rootNode)
        nodeToSongNetwork = NodeToSongNetwork()
        nodeToSongNetwork.addNodeOrSong(rootNode)
        incrementalPersonalizedPageRank = IncrementalPersonalizedPageRankMeritRank(MAX_WALK_LENGTH, ALPHA_REPETITIONS, rootNode, ALPHA_DECAY, BETA_DECAY, 0.95, nodeToNodeNetwork.graph, false)
        incrementalHybridPersonalizedPageRankSalsa = IncrementalHybridPersonalizedPageRankSalsaMeritRank2(MAX_WALK_LENGTH, BETA_REPETITIONS, rootNode, 0.1, 0.01, 0.01, EXPLORATION_PROBABILITY, nodeToSongNetwork.graph, nodeToNodeNetwork.graph, false)
    }

    open fun bulkAddNodeToSongEdgesForNode(edges: List<NodeSongEdgeWithNodeAndSongRec>, sourceNode: Node): Boolean {
        var edgeAdditionFailure = false
        if(edges.any { it.node != sourceNode })
            return false
        for(edge in edges) {
            var existingAffinity = 0.0
            if (containsEdge(edge.node, edge.songRec)) {
                val existingEdgeTimestamp = nodeToSongNetwork.graph.getEdge(edge.node, edge.songRec).timestamp
                existingAffinity = nodeToSongNetwork.graph.getEdge(edge.node, edge.songRec).affinity
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
                        updateNodeTrust(edge.songRec, edge.nodeSongEdge.affinity - existingAffinity)
                    }
                }
            }
        }
        incrementalPersonalizedPageRank.modifyEdges(setOf(sourceNode))
        incrementalHybridPersonalizedPageRankSalsa.modifyNodesOrSongs(setOf(sourceNode), nodeToNodeNetwork.getAllNodes().toList())
        return edgeAdditionFailure
    }
    fun bulkAddNodeToSongEdgesForExperiments(edges: List<NodeSongEdgeWithNodeAndSongRec>, sourceNode: Node): Boolean {
        var edgeAdditionFailure = false
        if(edges.any { it.node != sourceNode })
            return false
        for(edge in edges) {
            nodeToSongNetwork.addEdge(edge.node, edge.songRec, edge.nodeSongEdge).also {
                if(!it) {
                    logger.error { "Couldn't add edge from ${edge.node} to ${edge.songRec}" }
                    edgeAdditionFailure = true
                }
            }
        }
        return edgeAdditionFailure
    }

    fun resetRandomWalks() {
        incrementalPersonalizedPageRank.initiateRandomWalks()
        incrementalHybridPersonalizedPageRankSalsa.initiateRandomWalks()
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
                incrementalPersonalizedPageRank.modifyEdges(setOf(edge.node))
                incrementalHybridPersonalizedPageRankSalsa.modifyNodesOrSongs(setOf(edge.node), nodeToNodeNetwork.getAllNodes().toList())
            }
        }
    }

    protected open fun updateNodeTrust(songRec: SongRecommendation, affinityDelta: Double) {
        val recommenderNodeEdges = nodeToSongNetwork.graph.outgoingEdgesOf(songRec)
        val rootNeighborEdges = nodeToNodeNetwork.graph.outgoingEdgesOf(rootNode)
        for(recommenderNodeEdge in recommenderNodeEdges) {
            val recommenderNode = nodeToSongNetwork.graph.getEdgeSource(recommenderNodeEdge) as Node
            if(recommenderNode != rootNode) {
                val trustDelta = recommenderNodeEdge.affinity * affinityDelta
                val existingTrustEdgeToNode =
                    rootNeighborEdges.find { nodeToNodeNetwork.graph.getEdgeTarget(it) == recommenderNode }
                val existingTrust = existingTrustEdgeToNode?.trust ?: 0.0
                val newTrust = existingTrust + trustDelta
                addNodeToNodeEdge(NodeTrustEdgeWithSourceAndTarget(NodeTrustEdge(newTrust), rootNode, recommenderNode))
            }
        }
    }

    private fun updateNodeTrustForExperiment(sourceNode: Node, songRec: SongRecommendation, affinityDelta: Double) {
        val recommenderNodeEdges = nodeToSongNetwork.graph.outgoingEdgesOf(songRec)
        val rootNeighborEdges = nodeToNodeNetwork.graph.outgoingEdgesOf(rootNode)
        for(recommenderNodeEdge in recommenderNodeEdges) {
            val recommenderNode = nodeToSongNetwork.graph.getEdgeSource(recommenderNodeEdge) as Node
            if(recommenderNode != sourceNode) {
                val trustDelta = recommenderNodeEdge.affinity * affinityDelta
                val existingTrustEdgeToNode =
                    rootNeighborEdges.find { nodeToNodeNetwork.graph.getEdgeTarget(it) == recommenderNode }
                val existingTrust = existingTrustEdgeToNode?.trust ?: 0.0
                val newTrust = existingTrust + trustDelta
                addNodeToNodeEdge(NodeTrustEdgeWithSourceAndTarget(NodeTrustEdge(newTrust), sourceNode, recommenderNode))
            }
        }
    }

    fun addNodeToNodeEdge(edge: NodeTrustEdgeWithSourceAndTarget): Boolean {
        if(containsEdge(edge.sourceNode, edge.targetNode)) {
            val existingEdgeTimestamp = nodeToNodeNetwork.graph.getEdge(edge.sourceNode, edge.targetNode).timestamp
            if(existingEdgeTimestamp >= edge.nodeTrustEdge.timestamp) {
                return false
            }
        }
        val newSourceNode = !containsNode(edge.sourceNode)
        if (newSourceNode) {
            if (!addNode(edge.sourceNode)) {
                return false
            }
        }
        if (!containsNode(edge.targetNode)) {
            if (!addNode(edge.targetNode)) {
                return false
            }
        }
        return nodeToNodeNetwork.addEdge(edge.sourceNode, edge.targetNode, edge.nodeTrustEdge).also {
            if(!it) {
                logger.error { "Couldn't add edge from ${edge.sourceNode} to ${edge.targetNode}" }
            } else {
                if(!newSourceNode) {
                    incrementalPersonalizedPageRank.modifyEdges(setOf(edge.sourceNode))
                    incrementalHybridPersonalizedPageRankSalsa.modifyNodesOrSongs(setOf(edge.sourceNode), nodeToNodeNetwork.getAllNodes().toList())
                }
            }
        }
    }

    fun getAllNodeToNodeEdges(): List<NodeTrustEdge> {
        return nodeToNodeNetwork.getAllEdges().toList()
    }

    fun getAllNodeToSongEdges(): List<NodeSongEdge> {
        return nodeToSongNetwork.getAllEdges().toList()
    }

    protected fun containsEdge(source: Node, target: NodeOrSong): Boolean {
        return if(target is SongRecommendation) nodeToSongNetwork.graph.containsEdge(source, target) else nodeToNodeNetwork.graph.containsEdge(source, target as Node)
    }

    protected fun containsNode(node: Node): Boolean {
        return nodeToSongNetwork.graph.containsVertex(node) && nodeToSongNetwork.graph.containsVertex(node)
    }

    protected fun containsSongRec(songRec: SongRecommendation): Boolean {
        return nodeToSongNetwork.graph.containsVertex(songRec)
    }

    fun addNode(node: Node): Boolean {
        return nodeToSongNetwork.addNodeOrSong(node) && nodeToNodeNetwork.addNode(node)
    }

    fun addSongRec(songRec: SongRecommendation): Boolean {
        return nodeToSongNetwork.addNodeOrSong(songRec)
    }

    fun serialize(): SerializedSubNetworks {
        val serializedNodeToNodeNetwork = nodeToNodeNetwork.serialize()
        val serializedNodeToSongNetwork = nodeToSongNetwork.serializeCompact()
        return SerializedSubNetworks(serializedNodeToNodeNetwork, serializedNodeToSongNetwork)
    }

}
