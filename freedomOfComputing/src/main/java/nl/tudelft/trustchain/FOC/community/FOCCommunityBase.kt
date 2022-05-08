package nl.tudelft.trustchain.FOC.community

import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.eva.TransferException
import nl.tudelft.ipv8.messaging.eva.TransferProgress
import java.util.*

abstract class FOCCommunityBase : Community() {

    abstract var torrentMessagesList: ArrayList<Pair<Peer, FOCMessage>>

    abstract fun setEVAOnReceiveProgressCallback(
        f: (peer: Peer, info: String, progress: TransferProgress) -> Unit
    )

    abstract fun setEVAOnReceiveCompleteCallback(
        f: (peer: Peer, info: String, id: String, data: ByteArray?) -> Unit
    )

    abstract fun setEVAOnErrorCallback(
        f: (peer: Peer, exception: TransferException) -> Unit
    )

    abstract fun informAboutTorrent(torrentName: String)

    abstract fun sendAppRequest(torrentInfoHash: String, peer: Peer, uuid: String)
}
