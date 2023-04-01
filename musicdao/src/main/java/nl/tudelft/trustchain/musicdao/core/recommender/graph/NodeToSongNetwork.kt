package nl.tudelft.trustchain.musicdao.core.recommender.graph

import mu.KotlinLogging
import nl.tudelft.trustchain.musicdao.core.recommender.model.*
import org.jgrapht.graph.DefaultUndirectedWeightedGraph

class NodeToSongNetwork {
    var graph: DefaultUndirectedWeightedGraph<NodeOrSong, NodeSongEdge>
    private val logger = KotlinLogging.logger {}
    val initialized = false
    lateinit var sourceNode: Node

    companion object {
    }

    init {
    }

    constructor() {
        graph = DefaultUndirectedWeightedGraph<NodeOrSong, NodeSongEdge>(NodeSongEdge::class.java)
    }
    fun addNodeOrSong(nodeOrSong: NodeOrSong): Boolean {
        return graph.addVertex(nodeOrSong).also {
            if (!it) logger.error { "Couldn't add ${nodeOrSong.identifier} to node to song network" }
        }
    }

    fun addEdge(source: NodeOrSong, target: NodeOrSong, nodeSongEdge: NodeSongEdge): Boolean {
        if(source is Node && target is Node || source is SongRecommendation && target is SongRecommendation)
            return false
        if(!graph.containsVertex(source))
        {
            if(!addNodeOrSong(source)) {
                return false
            }
        }
        if(!graph.containsVertex(target))
        {
            if(!addNodeOrSong(target)) {
                return false
            }
        }
        if (graph.containsEdge(source, target)) {
            logger.info { "Overwriting edge from $source to $target" }
            graph.removeEdge(source, target)
        }
        return graph.addEdge(source, target, nodeSongEdge).also {
            if(!it) {
                logger.error { "Couldn't add edge $nodeSongEdge to network" }
            } else {
                graph.setEdgeWeight(nodeSongEdge, nodeSongEdge.affinity)
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
}
