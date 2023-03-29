package nl.tudelft.trustchain.musicdao.core.recommender.graph

import mu.KotlinLogging
import nl.tudelft.trustchain.musicdao.core.recommender.graph.customSerialization.CustomExporter
import nl.tudelft.trustchain.musicdao.core.recommender.graph.customSerialization.CustomImporter
import nl.tudelft.trustchain.musicdao.core.recommender.model.*
import nl.tudelft.trustchain.musicdao.core.recommender.randomwalk.RandomWalk
import org.jgrapht.graph.DefaultUndirectedWeightedGraph
import org.jgrapht.graph.SimpleDirectedWeightedGraph
import org.jgrapht.nio.Attribute
import org.jgrapht.nio.DefaultAttribute
import org.jgrapht.nio.json.JSONExporter
import org.jgrapht.nio.json.JSONImporter
import org.jgrapht.traverse.RandomWalkVertexIterator
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.io.StringWriter
import java.sql.Timestamp
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class TrustNetwork {
    var nodeToNodeNetwork: SimpleDirectedWeightedGraph<Node, NodeTrustEdge>
    lateinit var nodeToItemNetwork: DefaultUndirectedWeightedGraph<NodeOrSong, NodeSongEdge>
    private val logger = KotlinLogging.logger {}
    private val nodeNetworkJSONExporter = JSONExporter<Node, NodeTrustEdge>()
    private val customExporter = CustomExporter()
    val initialized = false
    lateinit var sourceNode: Node

    companion object {
        fun deserializeNodeToNodeJson(jsonString: String): SimpleDirectedWeightedGraph<Node, NodeTrustEdge> {
            val network = SimpleDirectedWeightedGraph<Node, NodeTrustEdge>(NodeTrustEdge::class.java)
            val importer = JSONImporter<Node, NodeTrustEdge>()
            importer.setVertexWithAttributesFactory { _: String, attrs: Map<String, Attribute> ->
                Node(attrs[Node.IPV8]!!.value, attrs[Node.PAGERANK]!!.value.toDouble())
            }
            importer.setEdgeWithAttributesFactory { attrs: Map<String, Attribute> ->
                NodeTrustEdge(
                    attrs[NodeTrustEdge.TRUST]!!.value.toDouble(),
                    Timestamp(attrs[NodeTrustEdge.TIMESTAMP]!!.value.toLong())
                )
            }
            importer.importGraph(network, StringReader(jsonString))
            return network
        }

        fun deserializeNodeToNodeCompact(compactString: String): SimpleDirectedWeightedGraph<Node, NodeTrustEdge> {
            val network = SimpleDirectedWeightedGraph<Node, NodeTrustEdge>(NodeTrustEdge::class.java)
            val importer = CustomImporter()
            importer.importGraph(network, StringReader(compactString))
            return network
        }
    }

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
        nodeToNodeNetwork = SimpleDirectedWeightedGraph<Node, NodeTrustEdge>(NodeTrustEdge::class.java)
        nodeToItemNetwork = DefaultUndirectedWeightedGraph<NodeOrSong, NodeSongEdge>(NodeSongEdge::class.java)
    }

    constructor(serializedGraph: String) {
        nodeToNodeNetwork = deserializeNodeToNodeJson(serializedGraph)
    }

    fun addNodeToNodeNetworkNode(node: Node): Boolean {
        return nodeToNodeNetwork.addVertex(node).also {
            if (!it) logger.error { "Couldn't add node ${node.ipv8} to node to node network" }
        }
    }
    fun addNodeToNodeNetworkEdge(source: Node, target: Node, nodeEdge: NodeTrustEdge): Boolean {
        if(!nodeToNodeNetwork.containsVertex(source))
        {
            if(!addNodeToNodeNetworkNode(source)) {
                logger.error { "Couldn't add edge $nodeEdge because source node couldn't be added" }
                return false
            }
        }
        if(!nodeToNodeNetwork.containsVertex(target))
        {
            if(!addNodeToNodeNetworkNode(target)) {
                logger.error { "Couldn't add edge $nodeEdge because target node couldn't be added" }
                return false
            }
        }
        if (nodeToNodeNetwork.containsEdge(source, target)) {
            logger.info { "Overwriting edge from ${source} to ${target}" }
            nodeToNodeNetwork.removeEdge(source, target)
        }
        return nodeToNodeNetwork.addEdge(source, target, nodeEdge).also {
            if(!it) {
                logger.error { "Couldn't add edge $nodeEdge to network" }
            } else {
                nodeToNodeNetwork.setEdgeWeight(nodeEdge, nodeEdge.trust)
            }
        }
    }

    fun getAllNodes(): Set<Node> {
        return nodeToNodeNetwork.vertexSet()
    }

    fun getAllNodeToNodeNetworkEdges(): Set<NodeTrustEdge> {
        return nodeToNodeNetwork.edgeSet()
    }

    fun serialize(): String {
        val stringWriter = StringWriter()
        nodeNetworkJSONExporter.exportGraph(nodeToNodeNetwork, stringWriter)
        return stringWriter.toString()
    }

    fun serializeCompactNodeNetwork(): String {
        val stringWriter = StringWriter()
        customExporter.exportGraph(nodeToNodeNetwork, stringWriter)
        return stringWriter.toString()
    }

    fun serializeCompactNodeNetworkZipped(): ByteArray {
        return gzip(serializeCompactNodeNetwork())
    }

    private fun gzip(content: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).bufferedWriter().use { it.write(content) }
        return bos.toByteArray()
    }

    private fun initPersonalizedPageRank(repetitions: Long, maxHops: Long) {
        //TODO: parallelize
        for (i in 0 until repetitions) {
            val iterator = RandomWalkVertexIterator(nodeToNodeNetwork, sourceNode, maxHops, true, Random())
            val randomWalk = RandomWalk<Node>()
            while (iterator.hasNext()) {
                randomWalk.addElement(iterator.next())
            }
        }
    }

    private fun ungzip(content: ByteArray): String =
        GZIPInputStream(content.inputStream()).bufferedReader().use { it.readText() }
}
