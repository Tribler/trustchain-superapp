package nl.tudelft.trustchain.musicdao.core.recommender.gossip

import kotlinx.serialization.json.Json
import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeRecEdge

class NodeToRecEdgeGossip(val edge: NodeRecEdge) : Serializable {
    override fun serialize(): ByteArray {
        return Json.encodeToString(NodeRecEdge.serializer(), edge).toByteArray()
    }

    companion object Deserializer : Deserializable<NodeToRecEdgeGossip> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<NodeToRecEdgeGossip, Int> {
            val tempStr = String(buffer, offset, buffer.size - offset)
            val edgeWithSourceAndTarget = Json.decodeFromString(NodeRecEdge.serializer(), tempStr)
            return Pair(NodeToRecEdgeGossip(edgeWithSourceAndTarget), offset)
        }
    }
}
