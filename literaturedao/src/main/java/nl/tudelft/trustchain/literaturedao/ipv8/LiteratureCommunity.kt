package nl.tudelft.trustchain.literaturedao.ipv8

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.frostwire.jlibtorrent.TorrentInfo
import mu.KotlinLogging
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCrawler
import nl.tudelft.ipv8.attestation.trustchain.TrustChainSettings
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.trustchain.literaturedao.utils.ExtensionUtils.Companion.supportedAppExtensions
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.eva.TransferException
import nl.tudelft.ipv8.messaging.eva.TransferProgress
import nl.tudelft.trustchain.literaturedao.LiteratureDaoActivity
import nl.tudelft.trustchain.literaturedao.model.remote_search.SearchResult
import nl.tudelft.trustchain.literaturedao.model.remote_search.SearchResultList
import nl.tudelft.trustchain.literaturedao.model.remote_search.SearchResultsMessage
import java.io.File
import java.io.FileOutputStream
import java.lang.Integer.min
import java.util.*
import kotlin.collections.ArrayList

private val logger = KotlinLogging.logger {}

@RequiresApi(Build.VERSION_CODES.N)
class LiteratureCommunity(
    context: Context,
    settings: TrustChainSettings,
    database: TrustChainStore,
    crawler: TrustChainCrawler = TrustChainCrawler()
) : TrustChainCommunity(settings, database, crawler) {
    override val serviceId: String = "0215eded9b27e6905a6d3fd02cc64d363c03a026"
    val discoveredAddressesContacted: MutableMap<IPv4Address, Date> = mutableMapOf()

    object MessageID {
        const val DEBUG_MESSAGE = 1
        const val SEARCH_QUERY = 2
        const val SEARCH_RESPONSE = 6
        const val TORRENT_MESSAGE = 3
        const val LITERATURE_REQUEST = 4
        const val LITERATURE = 5
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

    val context = context

    companion object {
        // Use this until we can commit an id to kotlin ipv8
        const val EVA_LITERATURE_COMMUNITY_ATTACHMENT = "eva_litdao_community_attachment"
    }

    override fun walkTo(address: IPv4Address) {
        super.walkTo(address)
        discoveredAddressesContacted[address] = Date()
    }

    fun broadcastDebugMessage(message: String) {
        for (peer in getPeers()) {
            val packet = serializePacket(MessageID.DEBUG_MESSAGE, DebugMessage(message))
            send(peer.address, packet)
        }
    }

    fun broadcastSearchQuery(query: String) {
        Log.d("litdao", "broadcasting remote query:\"$query\"")
        for (peer in getPeers()) {
            val packet = serializePacket(MessageID.SEARCH_QUERY, LitDaoMessage(query))
            send(peer, packet)
        }
    }

    private fun onDebugMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(DebugMessage.Deserializer)
        Log.i("litdao", peer.mid + ": " + payload.message)
    }

    /**
     * Received a remote query from other device.
     * Perform local search on the query and respond with a list of relevant docs.
     */
    private fun onSearchQueryMessage(packet: Packet) {
        val litDaoActivity = context as LiteratureDaoActivity

        // Decode packet
        val (peer, payload) = packet.getAuthPayload(LitDaoMessage)
        Log.d("litdao", "received remote query:\"" + payload.message + "\"")

        // Perform local search on the query
        val results = litDaoActivity.localSearch(payload.message)

        // Parse data and collect magnet links
        results.sortByDescending { it.second }
        results.take(min(results.size, 10))
        val parsed = results.map {
            SearchResult(
                it.first.localFileUri,
                it.second,
                litDaoActivity.createTorrent(it.first.localFileUri)!!.makeMagnetUri()
            )
        }

        Log.d("litdao", "replying remote query with list: " + results.toString())
        // Encode and send to peer
        val response = serializePacket(
            MessageID.SEARCH_RESPONSE,
            SearchResultsMessage(SearchResultList(parsed))
        )
        send(peer, response)
    }

    /**
     * Received relevant docs for my query
     */
    private fun onSearchResponseMessage(packet: Packet) {
        val litDaoActivity = context as LiteratureDaoActivity

        val (_, payload) = packet.getAuthPayload(SearchResultsMessage)

        Log.d("litdao", "Received remote query results list: " + payload.results.toString())

        litDaoActivity.updateSearchResults(payload.results)
    }

    private fun onTorrentMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(LitDaoMessage)
        val torrentHash = payload.message.substringAfter("magnet:?xt=urn:btih:")
            .substringBefore("&dn=")
        if (torrentMessagesList.none {
                it.second
                val existingHash =
                    it.second.message.substringAfter("magnet:?xt=urn:btih:").substringBefore("&dn=")
                torrentHash == existingHash
            }
        ) {
            torrentMessagesList.add(Pair(peer, payload))
            Log.i("litdao", peer.mid + ": " + payload.message)
        }
    }

    private fun onLiteratureRequestPacket(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(LiteratureRequestPayload.Deserializer)
        logger.debug { "-> LiteratureCommunity: Received request $payload from ${peer.mid}" }
        onLiteratureRequest(peer, payload.literatureTorrentInfoHash)
    }

    private fun onLiteratureRequest(peer: Peer, appTorrentInfoHash: String) {
        try {
            locateLiterature(appTorrentInfoHash)?.let { file ->
                logger.debug { "-> sending literature ${file.name} to ${peer.mid}" }
                sendLiterature(peer, appTorrentInfoHash, file)
                return
            }
            logger.debug { "Received Request for an literature that doesn't exist" }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun locateLiterature(literatureTorrentInfoHash: String): File? {
        appDirectory.listFiles()?.forEachIndexed { _, file ->
            if (file.name.endsWith(".torrent")) {
                TorrentInfo(file).let { torrentInfo ->
                    if (torrentInfo.infoHash().toString() == literatureTorrentInfoHash) {
                        if (torrentInfo.isValid) {
                            if (isTorrentOkay(torrentInfo, appDirectory)) {
                                return File(appDirectory.path + "/" + torrentInfo.name())
                            }
                        }
                    }
                }
            }
        }
        return null
    }

    private fun onLiteraturePacket(packet: Packet) {
        val (peer, payload) = packet.getDecryptedAuthPayload(
            LiteraturePayload.Deserializer, myPeer.key as PrivateKey
        )
        logger.debug { "<- Received literature from ${peer.mid}" }
        val file = appDirectory.toString() + "/" + payload.literatureName
        val existingFile = File(file)
        if (!existingFile.exists()) {
            try {
                val os = FileOutputStream(file)
                os.write(payload.data)
            } catch (e: Exception) {
                logger.debug { "Could not write file from $peer with hash ${payload.literatureTorrentInfoHash}" }
            }
        } else {
            logger.error { "File $file already exists, will not overwrite after EVA download" }
        }
    }

    private fun isTorrentOkay(torrentInfo: TorrentInfo, saveDirectory: File): Boolean {
        File(saveDirectory.path + "/" + torrentInfo.name()).run {
            if (!supportedAppExtensions.contains(extension)) return false
            if (length() >= torrentInfo.totalSize()) return true
        }
        return false
    }

    fun sendLiteratureRequest(torrentInfoHash: String, peer: Peer) {
        LiteratureRequestPayload(torrentInfoHash).let { payload ->
            logger.debug { "-> $payload" }
            send(peer, serializePacket(MessageID.LITERATURE_REQUEST, payload))
        }
    }

    private fun sendLiterature(peer: Peer, literatureTorrentInfoHash: String, file: File) {
        val literaturePayload =
            LiteraturePayload(literatureTorrentInfoHash, file.name, file.readBytes())
        val packet =
            serializePacket(
                MessageID.LITERATURE,
                literaturePayload,
                encrypt = true,
                recipient = peer
            )
        if (evaProtocolEnabled) evaSendBinary(
            peer,
            EVA_LITERATURE_COMMUNITY_ATTACHMENT,
            literatureTorrentInfoHash,
            packet
        ) else send(peer, packet)
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

    init {
        messageHandlers[MessageID.DEBUG_MESSAGE] = ::onDebugMessage
        messageHandlers[MessageID.SEARCH_QUERY] = ::onSearchQueryMessage
        messageHandlers[MessageID.SEARCH_RESPONSE] = ::onSearchResponseMessage
        messageHandlers[MessageID.TORRENT_MESSAGE] = ::onTorrentMessage
        messageHandlers[MessageID.LITERATURE_REQUEST] = ::onLiteratureRequestPacket
        messageHandlers[MessageID.LITERATURE] = ::onLiteraturePacket
        evaProtocolEnabled = true
    }

    override fun load() {
        super.load()

        setOnEVASendCompleteCallback(::onEVASendCompleteCallback)
        setOnEVAReceiveProgressCallback(::onEVAReceiveProgressCallback)
        setOnEVAReceiveCompleteCallback(::onEVAReceiveCompleteCallback)
        setOnEVAErrorCallback(::onEVAErrorCallback)
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

    private fun onEVASendCompleteCallback(peer: Peer, info: String, nonce: ULong) {
        Log.d("LiteratureCommunity", "ON EVA send complete callback for '$info'")

        if (info != EVA_LITERATURE_COMMUNITY_ATTACHMENT) return

        if (this::evaSendCompleteCallback.isInitialized) {
            this.evaSendCompleteCallback(peer, info, nonce)
        }
    }

    private fun onEVAReceiveProgressCallback(peer: Peer, info: String, progress: TransferProgress) {
        Log.d("LiteratureCommunity", "ON EVA receive progress callback for '$info'")

        if (info != EVA_LITERATURE_COMMUNITY_ATTACHMENT) return

        if (this::evaReceiveProgressCallback.isInitialized) {
            this.evaReceiveProgressCallback(peer, info, progress)
        }
    }

    private fun onEVAReceiveCompleteCallback(
        peer: Peer,
        info: String,
        id: String,
        data: ByteArray?
    ) {
        Log.d("LiteratureCommunity", "ON EVA receive complete callback for '$info'")

        if (info != EVA_LITERATURE_COMMUNITY_ATTACHMENT) return

        data?.let {
            val packet = Packet(peer.address, it)
            onLiteraturePacket(packet)
        }

        if (this::evaReceiveCompleteCallback.isInitialized) {
            this.evaReceiveCompleteCallback(peer, info, id, data)
        }
    }

    private fun onEVAErrorCallback(peer: Peer, exception: TransferException) {
        Log.d(
            "LiteratureCommunity",
            "ON EVA error callback for '${exception.info} from ${peer.mid}'"
        )

        if (this::evaErrorCallback.isInitialized) {
            this.evaErrorCallback(peer, exception)
        }
    }
}
