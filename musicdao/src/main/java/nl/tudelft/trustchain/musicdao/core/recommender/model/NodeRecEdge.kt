package nl.tudelft.trustchain.musicdao.core.recommender.model

import kotlinx.serialization.Serializable
import nl.tudelft.trustchain.musicdao.core.recommender.graph.customSerialization.Edge.NodeAsStringSerializer
import nl.tudelft.trustchain.musicdao.core.recommender.graph.customSerialization.Edge.RecAsStringSerializer

@Serializable
data class NodeRecEdge(
    val nodeSongEdge: NodeSongEdge,
    @Serializable(with = NodeAsStringSerializer::class)
    val node: Node,
    @Serializable(with = RecAsStringSerializer::class)
    val rec: Recommendation
)
