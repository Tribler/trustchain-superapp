package nl.tudelft.trustchain.musicdao.core.recommender.model

import kotlinx.serialization.Serializable
import nl.tudelft.trustchain.musicdao.core.recommender.graph.customSerialization.Edge.TimeStampAsLongSerializer
import org.jgrapht.graph.DefaultWeightedEdge
import java.sql.Timestamp

@Serializable
class NodeTrustEdge (
    val trust: Double = 0.0,
    @Serializable(with = TimeStampAsLongSerializer::class)
    val timestamp: Timestamp = Timestamp(System.currentTimeMillis())
): DefaultWeightedEdge() {
    companion object {
        val TRUST = NodeTrustEdge::trust.name
        val TIMESTAMP = NodeTrustEdge::timestamp.name
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }

    override fun toString(): String {
        return "($source : $target $trust v$timestamp)"
    }

    override fun equals(other: Any?): Boolean {
        return other is NodeTrustEdge && toString() == other.toString()
    }
}
