package nl.tudelft.trustchain.musicdao.core.recommender.gossip

import kotlinx.serialization.json.Json
import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeTrustEdgeWithSourceAndTarget

class NodeToNodeEdgeGossip(val edge: NodeTrustEdgeWithSourceAndTarget): Serializable {
    override fun serialize(): ByteArray {
        return Json.encodeToString(NodeTrustEdgeWithSourceAndTarget.serializer(), edge).toByteArray()
    }

    companion object Deserializer : Deserializable<NodeToNodeEdgeGossip> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<NodeToNodeEdgeGossip, Int> {
            val tempStr = String(buffer, offset,buffer.size - offset)
            val edgeWithSourceAndTarget = Json.decodeFromString(NodeTrustEdgeWithSourceAndTarget.serializer(), tempStr)
            return Pair(NodeToNodeEdgeGossip(edgeWithSourceAndTarget), offset)
        }
    }
}
