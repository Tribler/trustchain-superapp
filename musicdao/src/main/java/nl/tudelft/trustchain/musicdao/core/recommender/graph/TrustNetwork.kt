package nl.tudelft.trustchain.musicdao.core.recommender.graph

import mu.KotlinLogging
import nl.tudelft.trustchain.musicdao.core.recommender.graph.customSerialization.NodeToNodeNetwork.CustomExporter
import nl.tudelft.trustchain.musicdao.core.recommender.model.*
import org.jgrapht.graph.DefaultUndirectedWeightedGraph
import org.jgrapht.nio.Attribute
import org.jgrapht.nio.DefaultAttribute
import org.jgrapht.nio.json.JSONExporter
import java.util.*

class TrustNetwork {
    var nodeToNodeNetwork: NodeToNodeNetwork
    lateinit var nodeToSongNetwork: DefaultUndirectedWeightedGraph<NodeOrSong, NodeSongEdge>
    private val logger = KotlinLogging.logger {}
    private val nodeNetworkJSONExporter = JSONExporter<Node, NodeTrustEdge>()
    private val customExporter =
        CustomExporter()
    val initialized = false
    lateinit var sourceNode: Node

    init {
        nodeNetworkJSONExporter.setVertexAttributeProvider { v: Node ->
            val map: MutableMap<String, Attribute> = LinkedHashMap()
            map[Node.IPV8] = DefaultAttribute.createAttribute(v.ipv8)
            map[Node.PAGERANK] = DefaultAttribute.createAttribute(v.personalisedPageRank)
            map
        }
        nodeNetworkJSONExporter.setEdgeAttributeProvider { e: NodeTrustEdge ->
            val map: MutableMap<String, Attribute> = LinkedHashMap()
            map[NodeTrustEdge.TRUST] = DefaultAttribute.createAttribute(e.trust)
            map[NodeTrustEdge.TIMESTAMP] = DefaultAttribute.createAttribute(e.timestamp.time)
            map
        }
    }

    constructor() {
        nodeToNodeNetwork = NodeToNodeNetwork()
        nodeToSongNetwork = DefaultUndirectedWeightedGraph<NodeOrSong, NodeSongEdge>(NodeSongEdge::class.java)
    }

    constructor(serializedGraphs: Pair<String, String>) {
        nodeToNodeNetwork = NodeToNodeNetwork()
        nodeToSongNetwork = DefaultUndirectedWeightedGraph<NodeOrSong, NodeSongEdge>(NodeSongEdge::class.java)
    }


}
