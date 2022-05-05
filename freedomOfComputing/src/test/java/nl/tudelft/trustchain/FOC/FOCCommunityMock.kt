package nl.tudelft.trustchain.FOC

import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.eva.TransferException
import nl.tudelft.ipv8.messaging.eva.TransferProgress
import nl.tudelft.trustchain.FOC.community.FOCCommunityBase
import nl.tudelft.trustchain.FOC.community.FOCMessage
import java.util.*

@Suppress("deprecation")
@OptIn(ExperimentalUnsignedTypes::class)
class FOCCommunityMock(
    override val serviceId: String
) : FOCCommunityBase() {

    init {
        evaProtocolEnabled = true
    }

    override var torrentMessagesList = ArrayList<Pair<Peer, FOCMessage>>()
    var appRequests = ArrayList<Pair<String, Peer>>()
    var torrentsInformedAbout = ArrayList<String>()

    fun addTorrentMessages(message: Pair<Peer, FOCMessage>) {
        torrentMessagesList.add(message)
    }

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
