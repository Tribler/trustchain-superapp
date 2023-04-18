package nl.tudelft.trustchain.musicdao.core.recommender.model

import kotlinx.serialization.Serializable
import nl.tudelft.trustchain.musicdao.core.recommender.graph.customSerialization.Edge.NodeAsStringSerializer
import nl.tudelft.trustchain.musicdao.core.recommender.graph.customSerialization.Edge.SongRecAsStringSerializer

@Serializable
data class NodeSongEdgeWithNodeAndSongRec (
    val nodeSongEdge: NodeSongEdge,
    @Serializable(with = NodeAsStringSerializer::class)
    val node: Node,
    @Serializable(with = SongRecAsStringSerializer::class)
    val songRec: SongRecommendation
)
