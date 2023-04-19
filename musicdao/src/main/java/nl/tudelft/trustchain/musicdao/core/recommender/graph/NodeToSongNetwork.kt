package nl.tudelft.trustchain.musicdao.core.recommender.graph

import mu.KotlinLogging
import nl.tudelft.trustchain.musicdao.core.recommender.graph.customSerialization.NodeToSongNetwork.CustomImporter
import nl.tudelft.trustchain.musicdao.core.recommender.graph.customSerialization.NodeToSongNetwork.CustomExporter
import nl.tudelft.trustchain.musicdao.core.recommender.model.*
import org.jgrapht.graph.DefaultUndirectedWeightedGraph
import org.jgrapht.graph.SimpleDirectedWeightedGraph
import java.io.StringReader
import java.io.StringWriter

class NodeToSongNetwork {
    lateinit var graph: DefaultUndirectedWeightedGraph<NodeOrSong, NodeSongEdge>
    private val logger = KotlinLogging.logger {}
    val initialized = false
    lateinit var sourceNode: Node
    private val customExporter = CustomExporter()

    companion object {
    }

    init {
    }

    constructor() {
        graph = DefaultUndirectedWeightedGraph<NodeOrSong, NodeSongEdge>(NodeSongEdge::class.java)
    }

    constructor(serializedString: String) {
        val network = DefaultUndirectedWeightedGraph<NodeOrSong, NodeSongEdge>(NodeSongEdge::class.java)
        val importer =
            CustomImporter()
        importer.importGraph(network, StringReader(serializedString))
        this.graph = network
    }

    fun addNodeOrSong(nodeOrSong: NodeOrSong): Boolean {
        return graph.addVertex(nodeOrSong).also {
            if (!it) logger.error { "Couldn't add ${nodeOrSong.identifier} to node to song network" }
        }
    }

    fun addEdge(source: NodeOrSong, target: NodeOrSong, nodeSongEdge: NodeSongEdge): Boolean {
        if (source is Node && target is Node || source is SongRecommendation && target is SongRecommendation)
            return false
        if (!graph.containsVertex(source)) {
                logger.error { "Couldn't add edge $nodeSongEdge because source node doesn't exist" }
                return false
        }
        if (!graph.containsVertex(target)) {
            logger.error { "Couldn't add edge $nodeSongEdge because target song doesn't exist" }
                return false
        }
        if (graph.containsEdge(source, target)) {
            logger.info { "Overwriting edge from $source to $target" }
            graph.removeEdge(source, target)
        }
        return graph.addEdge(source, target, nodeSongEdge).also {
            if (!it) {
                logger.error { "Couldn't add edge $nodeSongEdge to network" }
            } else {
                graph.setEdgeWeight(nodeSongEdge, nodeSongEdge.affinity)
            }
        }
    }

    fun removeEdge(nodeEdge: NodeSongEdge): Boolean {
        return graph.removeEdge(nodeEdge).also {
            if (!it) {
                logger.error { "Couldn't remove edge $nodeEdge from network" }
            } else {
                graph.setEdgeWeight(nodeEdge, 0.0)
            }
        }
    }

    fun getAllNodes(): Set<Node> {
        return graph.vertexSet().filterIsInstance<Node>().toSet()
    }

    fun getAllSongs(): Set<SongRecommendation> {
        return graph.vertexSet().filterIsInstance<SongRecommendation>().toSet()
    }

    fun getAllEdges(): Set<NodeSongEdge> {
        return graph.edgeSet()
    }

    fun serializeCompact(): String {
        val stringWriter = StringWriter()
        customExporter.exportGraph(graph, stringWriter)
        return stringWriter.toString()
    }
}
