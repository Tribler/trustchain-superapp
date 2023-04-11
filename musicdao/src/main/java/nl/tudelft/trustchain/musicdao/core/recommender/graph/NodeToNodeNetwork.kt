package nl.tudelft.trustchain.musicdao.core.recommender.graph

import mu.KotlinLogging
import nl.tudelft.trustchain.musicdao.core.recommender.graph.customSerialization.NodeToNodeNetwork.CustomExporter
import nl.tudelft.trustchain.musicdao.core.recommender.graph.customSerialization.NodeToNodeNetwork.CustomImporter
import nl.tudelft.trustchain.musicdao.core.recommender.model.*
import org.jgrapht.graph.SimpleDirectedWeightedGraph
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.io.StringWriter
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class NodeToNodeNetwork {
    var graph: SimpleDirectedWeightedGraph<Node, NodeTrustEdge>
    private val logger = KotlinLogging.logger {}
    private val customExporter = CustomExporter()
    val initialized = false
    lateinit var sourceNode: Node


    constructor() {
        graph = SimpleDirectedWeightedGraph<Node, NodeTrustEdge>(NodeTrustEdge::class.java)
    }

    constructor(network: SimpleDirectedWeightedGraph<Node, NodeTrustEdge>) {
        this.graph = network
    }

    constructor(serializedString: String) {
        val network = SimpleDirectedWeightedGraph<Node, NodeTrustEdge>(NodeTrustEdge::class.java)
        val importer =
            CustomImporter()
        importer.importGraph(network, StringReader(serializedString))
        this.graph = network
    }

    fun addNode(node: Node): Boolean {
        return graph.addVertex(node).also {
            if (!it) logger.error { "Couldn't add node ${node.ipv8} to node to node network" }
        }
    }

    fun addEdge(source: Node, target: Node, nodeEdge: NodeTrustEdge): Boolean {
        if (!graph.containsVertex(source)) {
            if (!addNode(source)) {
                logger.error { "Couldn't add edge $nodeEdge because source node couldn't be added" }
                return false
            }
        }
        if (!graph.containsVertex(target)) {
            if (!addNode(target)) {
                logger.error { "Couldn't add edge $nodeEdge because target node couldn't be added" }
                return false
            }
        }
        if (graph.containsEdge(source, target)) {
            logger.info { "Overwriting edge from ${source} to ${target}" }
            graph.removeEdge(source, target)
        }
        return graph.addEdge(source, target, nodeEdge).also {
            if (!it) {
                logger.error { "Couldn't add edge $nodeEdge to network" }
            } else {
                graph.setEdgeWeight(nodeEdge, nodeEdge.trust)
            }
        }
    }

    fun getAllNodes(): Set<Node> {
        return graph.vertexSet()
    }

    fun getAllNodeToNodeNetworkEdges(): Set<NodeTrustEdge> {
        return graph.edgeSet()
    }

    fun serialize(): String {
        val stringWriter = StringWriter()
        customExporter.exportGraph(graph, stringWriter)
        return stringWriter.toString()
    }

    fun serializeZipped(): ByteArray {
        return gzip(serialize())
    }

    private fun gzip(content: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).bufferedWriter().use { it.write(content) }
        return bos.toByteArray()
    }

    private fun ungzip(content: ByteArray): String =
        GZIPInputStream(content.inputStream()).bufferedReader().use { it.readText() }
}
