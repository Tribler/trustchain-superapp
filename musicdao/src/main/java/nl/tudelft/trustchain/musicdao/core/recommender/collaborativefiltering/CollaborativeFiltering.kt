package nl.tudelft.trustchain.musicdao.core.recommender.collaborativefiltering

import nl.tudelft.trustchain.musicdao.core.recommender.model.Node
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeRecEdge

interface CollaborativeFiltering {
    fun similarNodes(nodeToSongEdges: List<NodeRecEdge>, size: Int): Map<Node, Double>
}
