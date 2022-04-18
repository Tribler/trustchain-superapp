package nl.tudelft.trustchain.literaturedao.ipv8
import android.content.Context
import android.util.Log
import mu.KotlinLogging
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCrawler
import nl.tudelft.ipv8.attestation.trustchain.TrustChainSettings
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.eva.TransferException
import nl.tudelft.ipv8.messaging.eva.TransferProgress

private val logger = KotlinLogging.logger {}

class LiteratureCommunity(
    context: Context,
    settings: TrustChainSettings,
    database: TrustChainStore,
    crawler: TrustChainCrawler = TrustChainCrawler()
) : TrustChainCommunity(settings, database, crawler)  {
    override val serviceId: String = "0215eded9b27e6905a6d3fd02cc64d363c03a026"

    object MessageID {
        const val DEBUG_MESSAGE = 1
        const val SEARCH_QUERY = 2
        const val TORRENT_MESSAGE = 3
        const val LITERATURE_REQUEST = 4
    }

    class Factory(
        private val context: Context,
        private val settings: TrustChainSettings,
        private val database: TrustChainStore,
        private val crawler: TrustChainCrawler = TrustChainCrawler()
    ) : Overlay.Factory<LiteratureCommunity>(LiteratureCommunity::class.java) {
        override fun create(): LiteratureCommunity {
            return LiteratureCommunity(context, settings, database, crawler)
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun broadcastDebugMessage(message: String) {
        for (peer in getPeers()) {
            val packet = serializePacket(MessageID.DEBUG_MESSAGE, DebugMessage(message))
            send(peer.address, packet)
        }
    }


    private fun onDebugMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(DebugMessage.Deserializer)
        Log.i("litdao", peer.mid + ": " + payload.message)
    }

    private fun onQueryMessage(packet: Packet) {
        // handle query message here
    }

    init {
        messageHandlers[MessageID.DEBUG_MESSAGE] = ::onDebugMessage
        messageHandlers[MessageID.SEARCH_QUERY] = ::onQueryMessage
    }

    // Eva protocol implementation. To override from FOCCommunityBase
    private val appDirectory = context.cacheDir
    var torrentMessagesList = ArrayList<Pair<Peer, LitDaoMessage>>()

    private lateinit var evaSendCompleteCallback: (
        peer: Peer,
        info: String,
        nonce: ULong
    ) -> Unit
    private lateinit var evaReceiveProgressCallback: (
        peer: Peer,
        info: String,
        progress: TransferProgress
    ) -> Unit
    private lateinit var evaReceiveCompleteCallback: (
        peer: Peer,
        info: String,
        id: String,
        data: ByteArray?
    ) -> Unit
    private lateinit var evaErrorCallback: (
        peer: Peer,
        exception: TransferException
    ) -> Unit

    fun setEVAOnReceiveCompleteCallback(
        f: (peer: Peer, info: String, id: String, data: ByteArray?) -> Unit
    ) {
        this.evaReceiveCompleteCallback = f
    }

    fun setEVAOnReceiveProgressCallback(
        f: (peer: Peer, info: String, progress: TransferProgress) -> Unit
    ) {
        this.evaReceiveProgressCallback = f
    }

    fun setEVAOnErrorCallback(
        f: (peer: Peer, exception: TransferException) -> Unit
    ) {
        this.evaErrorCallback = f
    }

    fun informAboutTorrent(torrentName: String) {
        if (torrentName != "") {
            for (peer in getPeers()) {
                val packet = serializePacket(
                    MessageID.TORRENT_MESSAGE,
                    LitDaoMessage("LitDao:$torrentName"),
                    true
                )
                send(peer.address, packet)
            }
        }
    }

    fun sendLiteratureRequest(torrentInfoHash: String, peer: Peer) {
        LiteratureRequestPayload(torrentInfoHash).let { payload ->
            logger.debug { "-> $payload" }
            send(peer, serializePacket(MessageID.LITERATURE_REQUEST, payload))
        }
    }


}
