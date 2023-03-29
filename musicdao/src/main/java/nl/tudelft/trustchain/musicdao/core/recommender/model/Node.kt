package nl.tudelft.trustchain.musicdao.core.recommender.model

import kotlinx.serialization.Serializable
@Serializable
data class Node(
    val ipv8: String,
    val personalisedPageRank: Double = 0.0
):NodeOrSong(identifier = ipv8) {

    companion object {
        val IPV8 = Node::ipv8.name
        val PAGERANK = "pr"
    }

    fun getNodeString(): String {
        return ipv8
    }
    override fun toString(): String {
        return ipv8
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is Node && toString() == other.toString()
    }
}
