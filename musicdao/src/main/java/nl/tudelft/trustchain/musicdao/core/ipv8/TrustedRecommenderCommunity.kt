package nl.tudelft.trustchain.musicdao.core.ipv8

import android.content.Context
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.trustchain.musicdao.core.recommender.gossip.NodeToNodeEdgeGossip
import nl.tudelft.trustchain.musicdao.core.recommender.gossip.NodeToRecEdgeGossip
import nl.tudelft.trustchain.musicdao.core.recommender.gossip.TIME_WINDOW
import nl.tudelft.trustchain.musicdao.core.recommender.model.Node
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeRecEdge
import nl.tudelft.trustchain.musicdao.core.recommender.model.NodeTrustEdgeWithSourceAndTarget
import nl.tudelft.trustchain.musicdao.core.recommender.model.Recommendation
import nl.tudelft.trustchain.musicdao.core.recommender.networks.SongRecTrustNetwork

class TrustedRecommenderCommunity(
    context: Context
) : Community() {
    override val serviceId = "12313685c1912a141279f8248fc8db5899c5df6c"

//    lateinit val songRecTrustNetwork = SongRecTrustNetwork.getInstance(myPeer.key.pub().toString(), context.cacheDir.path.toString())

    fun sendNodeToNodeEdges(peer: Peer, nodeToNodeEdges: List<NodeTrustEdgeWithSourceAndTarget>) {
        for(edge in nodeToNodeEdges) {
            val packet =
                serializePacket(MessageId.NODE_TO_NODE_EDGE, NodeToNodeEdgeGossip(edge), encrypt = true, recipient = peer)
            send(peer, packet)
        }
    }

    fun sendNodeToSongEdges(peer: Peer, nodeToSongEdges: List<NodeRecEdge>) {
        for(edge in nodeToSongEdges) {
            val packet =
                serializePacket(MessageId.NODE_TO_REC_EDGE, NodeToRecEdgeGossip(edge), encrypt = true, recipient = peer)
            send(peer, packet)
        }
    }

    object MessageId {
        const val NODE_TO_NODE_EDGE = 400
        const val NODE_TO_REC_EDGE = 401
    }

    class Factory(
        private val context: Context
    ) : Overlay.Factory<TrustedRecommenderCommunity>(TrustedRecommenderCommunity::class.java) {
        override fun create(): TrustedRecommenderCommunity {
            return TrustedRecommenderCommunity(context)
        }
    }


}
