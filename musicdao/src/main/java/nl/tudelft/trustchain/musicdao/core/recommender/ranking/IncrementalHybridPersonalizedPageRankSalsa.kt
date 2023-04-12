package nl.tudelft.trustchain.musicdao.core.recommender.ranking

import mu.KotlinLogging
import nl.tudelft.trustchain.musicdao.core.recommender.model.Node
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeOrSong
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeSongEdge
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeTrustEdge
import nl.tudelft.trustchain.musicdao.core.recommender.ranking.iterator.CustomHybridRandomWalkIterator
import nl.tudelft.trustchain.musicdao.core.recommender.ranking.iterator.CustomRandomWalkVertexIterator
import org.jgrapht.graph.DefaultUndirectedWeightedGraph
import org.jgrapht.graph.SimpleDirectedWeightedGraph
import java.util.*

class IncrementalHybridPersonalizedPageRankSalsa (
    private val maxWalkLength: Int,
    private val repetitions: Int,
    private val rootNode: Node,
    resetProbability: Float,
    graph: DefaultUndirectedWeightedGraph<NodeOrSong, NodeSongEdge>
) {
    private val logger = KotlinLogging.logger {}
    private val iter = CustomHybridRandomWalkIterator(graph, rootNode, maxWalkLength.toLong(), resetProbability, Random())
    private val randomWalks: MutableList<MutableList<NodeOrSong>> = mutableListOf()
}
