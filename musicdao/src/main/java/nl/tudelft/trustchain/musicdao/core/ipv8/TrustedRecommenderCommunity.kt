package nl.tudelft.trustchain.musicdao.core.ipv8

import android.content.Context
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.trustchain.musicdao.core.recommender.gossip.NodeToNodeEdgeGossip
import nl.tudelft.trustchain.musicdao.core.recommender.gossip.NodeToRecEdgeGossip
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeRecEdge
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeTrustEdgeWithSourceAndTarget
import nl.tudelft.trustchain.musicdao.core.recommender.networks.SongRecTrustNetwork
import java.sql.Timestamp
import java.util.ArrayList

class TrustedRecommenderCommunity(
    context: Context
) : Community() {
    private val appDirectory = context.cacheDir
    override val serviceId = "12313685c1912a141279f8248fc8db5899c5df6c"

    lateinit var trustNetwork: SongRecTrustNetwork

    init {
        messageHandlers[MessageId.NODE_TO_NODE_EDGE] = ::onNodeToNodeEdge
        messageHandlers[MessageId.NODE_TO_REC_EDGE] = ::onNodeToRecEdge
    }

    fun sendNodeToNodeEdges(peer: Peer, nodeToNodeEdges: List<NodeTrustEdgeWithSourceAndTarget>) {
        for(edge in nodeToNodeEdges) {
            val packet =
                serializePacket(MessageId.NODE_TO_NODE_EDGE, NodeToNodeEdgeGossip(edge), sign = false)
            send(peer, packet)
        }
    }

    fun sendNodeRecEdges(peer: Peer, nodeToSongEdges: List<NodeRecEdge>) {
        for(edge in nodeToSongEdges) {
            val packet =
                serializePacket(MessageId.NODE_TO_REC_EDGE, NodeToRecEdgeGossip(edge), sign = false)
            send(peer, packet)
        }
    }

    private fun onNodeToNodeEdge(packet: Packet) {
        if(!::trustNetwork.isInitialized) {
            trustNetwork = SongRecTrustNetwork.getInstance(myPeer.key.pub().toString(), appDirectory.path.toString())
        }
        val payload = packet.getAuthPayload(NodeToNodeEdgeGossip.Deserializer).second.edge
        if(trustNetwork.nodeToNodeNetwork.graph.containsVertex(payload.sourceNode)) {
            val existingEdges = trustNetwork.nodeToNodeNetwork.graph.outgoingEdgesOf(payload.sourceNode)
            if (existingEdges.size > 4) {
                val oldestEdge = existingEdges.minByOrNull { it.timestamp }!!
                if(oldestEdge.timestamp > payload.nodeTrustEdge.timestamp)
                    return
                trustNetwork.nodeToNodeNetwork.removeEdge(oldestEdge)
            }
        } else {
            trustNetwork.addNode(payload.sourceNode)
        }
        if(!trustNetwork.nodeToNodeNetwork.graph.containsVertex(payload.targetNode)) {
            trustNetwork.addNode(payload.targetNode)
        }
        trustNetwork.addNodeToNodeEdge(payload)
    }
    private fun onNodeToRecEdge(packet: Packet) {
        if(!::trustNetwork.isInitialized) {
            trustNetwork = SongRecTrustNetwork.getInstance(myPeer.key.pub().toString(), appDirectory.path.toString())
        }
        val payload = packet.getPayload(NodeToRecEdgeGossip.Deserializer).edge
        if(trustNetwork.nodeToSongNetwork.graph.containsVertex(payload.node)) {
            val existingEdges = trustNetwork.nodeToSongNetwork.graph.outgoingEdgesOf(payload.node)
            if (existingEdges.size > 4) {
                val oldestEdge = existingEdges.minByOrNull { it.timestamp }!!
                if(oldestEdge.timestamp > payload.nodeSongEdge.timestamp)
                    return
                trustNetwork.nodeToSongNetwork.removeEdge(oldestEdge)
            }
        } else {
            trustNetwork.addNode(payload.node)
        }
        if(!trustNetwork.nodeToSongNetwork.graph.containsVertex(payload.rec)) {
            trustNetwork.addSongRec(payload.rec)
        }
        trustNetwork.addNodeToSongEdge(payload)
    }

    object MessageId {
        const val NODE_TO_NODE_EDGE = 1
        const val NODE_TO_REC_EDGE = 2
    }

    class Factory(
        private val context: Context
    ) : Overlay.Factory<TrustedRecommenderCommunity>(TrustedRecommenderCommunity::class.java) {
        override fun create(): TrustedRecommenderCommunity {
            return TrustedRecommenderCommunity(context)
        }
    }
}
