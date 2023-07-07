package nl.tudelft.trustchain.musicdao.core.recommender.model

import kotlinx.serialization.Serializable
import nl.tudelft.trustchain.musicdao.core.recommender.graph.customSerialization.Edge.TimeStampAsLongSerializer
import org.jgrapht.graph.DefaultWeightedEdge
import java.sql.Timestamp

@Serializable
class NodeSongEdge(
    val affinity: Double = 0.0,
    @Serializable(with = TimeStampAsLongSerializer::class)
    val timestamp: Timestamp = Timestamp(System.currentTimeMillis())
) : DefaultWeightedEdge() {
    companion object {
        const val TRUST = "trust"
        const val VERSION = "version"
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }

    override fun toString(): String {
        return "($source : $target $affinity $timestamp)"
    }

    override fun equals(other: Any?): Boolean {
        return other is NodeSongEdge && toString() == other.toString()
    }
}
