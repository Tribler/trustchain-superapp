package nl.tudelft.trustchain.musicdao.recommender

import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.eva.TransferException
import nl.tudelft.ipv8.messaging.eva.TransferProgress
import nl.tudelft.trustchain.musicdao.core.recommender.gossip.NodeToNodeEdgeGossip
import nl.tudelft.trustchain.musicdao.core.recommender.gossip.RecommenderCommunityBase
import java.util.*

@Suppress("deprecation")
class RecommenderCommunityMock(
    override val serviceId: String
) : RecommenderCommunityBase() {

    init {
        evaProtocolEnabled = true
    }

    override var edgeGossipList = ArrayList<Pair<Peer, NodeToNodeEdgeGossip>>()
    var appRequests = ArrayList<Pair<String, Peer>>()
    var torrentsInformedAbout = ArrayList<String>()

    override fun setEVAOnReceiveProgressCallback(
        f: (
            peer: Peer,
            info: String,
            progress: TransferProgress
        ) -> Unit
    ) {
    }

    override fun setEVAOnReceiveCompleteCallback(
        f: (
            peer: Peer,
            info: String,
            id: String,
            data: ByteArray?
        ) -> Unit
    ) {
    }

    override fun setEVAOnErrorCallback(
        f: (
            peer: Peer,
            exception: TransferException
        ) -> Unit
    ) {
    }

    override fun informAboutTorrent(torrentName: String) {
        torrentsInformedAbout.add(torrentName)
    }

    override fun sendAppRequest(torrentInfoHash: String, peer: Peer, uuid: String) {
        appRequests.add(Pair(torrentInfoHash, peer))
    }
}
