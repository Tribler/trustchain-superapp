package nl.tudelft.trustchain.musicdao.core.recommender.graph

import mu.KotlinLogging
import nl.tudelft.trustchain.musicdao.core.recommender.model.*
import nl.tudelft.trustchain.musicdao.core.recommender.ranking.IncrementalPersonalizedPageRank



class TrustNetwork {
    private val nodeToNodeNetwork: NodeToNodeNetwork
    private val nodeToSongNetwork: NodeToSongNetwork
    private val incrementalPersonalizedPageRank: IncrementalPersonalizedPageRank
    private val allNodes: MutableList<Node>
    private val logger = KotlinLogging.logger {}
    val rootNode: Node

    companion object {
        const val MAX_WALK_LENGTH = 1000
        const val REPETITIONS = 10000
        const val RESET_PROBABILITY = 0.01f

    }
    constructor(sourceNodeAddress: String) {
        nodeToNodeNetwork = NodeToNodeNetwork()
        nodeToSongNetwork = NodeToSongNetwork()
        rootNode = Node(sourceNodeAddress)
        nodeToNodeNetwork.addNode(rootNode)
        nodeToSongNetwork.addNodeOrSong(rootNode)
        allNodes = mutableListOf(rootNode)
        incrementalPersonalizedPageRank = IncrementalPersonalizedPageRank(MAX_WALK_LENGTH, REPETITIONS, rootNode, RESET_PROBABILITY, nodeToNodeNetwork.graph)
    }

    constructor(serializedGraphs: SerializedGraphs, sourceNodeAddress: String) {
        nodeToNodeNetwork = NodeToNodeNetwork(serializedGraphs.nodeToNodeNetwork)
        nodeToSongNetwork = NodeToSongNetwork(serializedGraphs.nodeToSongNetwork)
        val allNodesList = nodeToNodeNetwork.getAllNodes()
        allNodes = allNodesList.toMutableList()
        rootNode = allNodes.first { it.getIpv8() == sourceNodeAddress}
        incrementalPersonalizedPageRank = IncrementalPersonalizedPageRank(MAX_WALK_LENGTH, REPETITIONS, rootNode, RESET_PROBABILITY, nodeToNodeNetwork.graph)
    }


}
