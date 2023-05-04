package nl.tudelft.trustchain.musicdao.core.recommender.collaborativefiltering

import nl.tudelft.trustchain.musicdao.core.recommender.model.Node
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeSongEdgeWithNodeAndSongRec

interface CollaborativeFiltering {
    fun similarNodes(nodeToSongEdges: List<NodeSongEdgeWithNodeAndSongRec>, size: Int): Map<Node, Double>
}
