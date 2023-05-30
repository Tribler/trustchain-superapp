package nl.tudelft.trustchain.musicdao.core.recommender.gossip

import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.eva.TransferException
import nl.tudelft.ipv8.messaging.eva.TransferProgress
import java.util.ArrayList


abstract class RecommenderCommunityBase : Community() {

    abstract var edgeGossipList: ArrayList<Pair<Peer, NodeToNodeEdgeGossip>>

    abstract fun sendNodeToNodeEdges(torrentName: String)

    abstract fun sendNodeToSongEdges(torrentInfoHash: String, peer: Peer, uuid: String)
}
