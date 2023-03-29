package nl.tudelft.trustchain.musicdao.core.recommender.model

import kotlinx.serialization.Serializable
import org.jgrapht.graph.DefaultWeightedEdge
import java.sql.Timestamp

@Serializable
class NodeSongEdge (
    val weight: Float = 0.0f,
    val timestamp: Timestamp = Timestamp(System.currentTimeMillis())
): DefaultWeightedEdge() {
    fun getTimestampString(): String {
        return timestamp.toString()
    }
    companion object {
        const val TRUST = "trust"
        const val VERSION = "version"
    }
    override fun getWeight(): Double {
        return weight.toDouble()
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }

    override fun toString(): String {
        return "($source : $target $weight $timestamp)"
    }

    override fun equals(other: Any?): Boolean {
        return other is NodeSongEdge && toString() == other.toString()
    }
}
