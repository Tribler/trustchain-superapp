package nl.tudelft.trustchain.musicdao.core.recommender.model

import kotlinx.serialization.Serializable
import nl.tudelft.trustchain.musicdao.core.recommender.graph.customSerialization.Edge.NodeAsStringSerializer

@Serializable
data class NodeTrustEdgeWithSourceAndTarget (
    val nodeTrustEdge: NodeTrustEdge,
    @Serializable(with = NodeAsStringSerializer::class)
    val sourceNode: Node,
    @Serializable(with = NodeAsStringSerializer::class)
    val targetNode: Node
)
