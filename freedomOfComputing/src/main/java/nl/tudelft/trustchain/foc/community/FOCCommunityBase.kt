package nl.tudelft.trustchain.foc.community

import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.eva.TransferException
import nl.tudelft.ipv8.messaging.eva.TransferProgress
import java.util.*
import kotlin.collections.ArrayList

abstract class FOCCommunityBase : Community() {
    abstract var torrentMessagesList: ArrayList<Pair<Peer, FOCMessage>>
    abstract var voteMessagesQueue: Queue<Pair<Peer, FOCVoteMessage>>
    abstract var pullVoteMessagesSendQueue: Queue<Peer>
    abstract var pullVoteMessagesReceiveQueue: Queue<FOCPullVoteMessage>

    abstract fun setEVAOnReceiveProgressCallback(f: (peer: Peer, info: String, progress: TransferProgress) -> Unit)

    abstract fun setEVAOnReceiveCompleteCallback(f: (peer: Peer, info: String, id: String, data: ByteArray?) -> Unit)

    abstract fun setEVAOnErrorCallback(f: (peer: Peer, exception: TransferException) -> Unit)

    abstract fun informAboutTorrent(torrentName: String)

    abstract fun informAboutVote(
        fileName: String,
        vote: FOCVote,
        ttl: UInt
    )

    abstract fun informAboutPullSendVote()

    abstract fun informAboutPullReceiveVote(
        voteMap: HashMap<String, HashSet<FOCVote>>,
        originPeer: Peer
    )

    abstract fun sendAppRequest(
        torrentInfoHash: String,
        peer: Peer,
        uuid: String
    )
}
