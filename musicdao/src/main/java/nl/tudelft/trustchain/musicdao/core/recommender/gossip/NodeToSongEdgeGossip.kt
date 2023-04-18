package nl.tudelft.trustchain.musicdao.core.recommender.gossip

import kotlinx.serialization.json.Json
import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeSongEdgeWithNodeAndSongRec

class NodeToSongEdgeGossip(val edge: NodeSongEdgeWithNodeAndSongRec): Serializable {
    override fun serialize(): ByteArray {
        return Json.encodeToString(NodeSongEdgeWithNodeAndSongRec.serializer(), edge).toByteArray()
    }

    companion object Deserializer : Deserializable<NodeToSongEdgeGossip> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<NodeToSongEdgeGossip, Int> {
            val tempStr = String(buffer, offset,buffer.size - offset)
            val edgeWithSourceAndTarget = Json.decodeFromString(NodeSongEdgeWithNodeAndSongRec.serializer(), tempStr)
            return Pair(NodeToSongEdgeGossip(edgeWithSourceAndTarget), offset)
        }
    }
}
