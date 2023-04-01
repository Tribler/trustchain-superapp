package nl.tudelft.trustchain.musicdao.core.recommender.model

import kotlinx.serialization.Serializable
import org.jgrapht.graph.DefaultWeightedEdge
import java.sql.Timestamp

@Serializable
class NodeSongEdge (
    val affinity: Double = 0.0,
    val timestamp: Timestamp = Timestamp(System.currentTimeMillis())
): DefaultWeightedEdge() {
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
