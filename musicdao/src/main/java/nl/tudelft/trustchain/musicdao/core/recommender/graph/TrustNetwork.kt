package nl.tudelft.trustchain.musicdao.core.recommender.graph

import mu.KotlinLogging
import nl.tudelft.trustchain.musicdao.core.recommender.model.*

class TrustNetwork {
    private val nodeToNodeNetwork: NodeToNodeNetwork
    private val nodeToSongNetwork: NodeToSongNetwork
    private val allNodes: MutableList<Node>
    private val logger = KotlinLogging.logger {}
    val initialized = false
    val sourceNode: Node

    constructor(sourceNodeAddress: String) {
        nodeToNodeNetwork = NodeToNodeNetwork()
        nodeToSongNetwork = NodeToSongNetwork()
        sourceNode = Node(sourceNodeAddress)
        nodeToNodeNetwork.addNode(sourceNode)
        nodeToSongNetwork.addNodeOrSong(sourceNode)
        allNodes = mutableListOf(sourceNode)
    }

    constructor(serializedGraphs: SerializedGraphs, sourceNodeAddress: String) {
        nodeToNodeNetwork = NodeToNodeNetwork(serializedGraphs.nodeToNodeNetwork)
        nodeToSongNetwork = NodeToSongNetwork(serializedGraphs.nodeToSongNetwork)
        val allNodesList = nodeToNodeNetwork.getAllNodes()
        allNodes = allNodesList.toMutableList()
        sourceNode = allNodes.first { it.ipv8 == sourceNodeAddress}
    }


}
