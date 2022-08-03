package nl.tudelft.trustchain.FOC.community

import android.content.Context
import android.util.Log
import com.frostwire.jlibtorrent.TorrentInfo
import mu.KotlinLogging
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.eva.TransferException
import nl.tudelft.ipv8.messaging.eva.TransferProgress
import nl.tudelft.trustchain.common.freedomOfComputing.AppPayload
import nl.tudelft.trustchain.common.freedomOfComputing.AppRequestPayload
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.util.*

private val logger = KotlinLogging.logger {}

@OptIn(ExperimentalUnsignedTypes::class)
class FOCCommunity(
    context: Context
) : FOCCommunityBase() {
    override val serviceId = "12313685c1912a141279f8248fc8db5899c5df5b"

    val discoveredAddressesContacted: MutableMap<IPv4Address, Date> = mutableMapOf()

    private val appDirectory = context.cacheDir

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

    override fun setEVAOnReceiveProgressCallback(
        f: (peer: Peer, info: String, progress: TransferProgress) -> Unit
    ) {
        this.evaReceiveProgressCallback = f
    }

    override fun setEVAOnReceiveCompleteCallback(
        f: (peer: Peer, info: String, id: String, data: ByteArray?) -> Unit
    ) {
        this.evaReceiveCompleteCallback = f
    }

    override fun setEVAOnErrorCallback(
        f: (peer: Peer, exception: TransferException) -> Unit
    ) {
        this.evaErrorCallback = f
    }

    // Retrieve the trustchain community
    private fun getTrustChainCommunity(): TrustChainCommunity {
        return IPv8Android.getInstance().getOverlay()
            ?: throw IllegalStateException("TrustChainCommunity is not configured")
    }

    override fun walkTo(address: IPv4Address) {
        super.walkTo(address)

        discoveredAddressesContacted[address] = Date()
    }

    override var torrentMessagesList = ArrayList<Pair<Peer, FOCMessage>>()

    object MessageId {
        const val FOC_THALIS_MESSAGE = 220
        const val TORRENT_MESSAGE = 230
        const val APP_REQUEST = 231
        const val APP = 232
    }

    override fun informAboutTorrent(torrentName: String) {
        if (torrentName != "") {
            for (peer in getPeers()) {
                val packet = serializePacket(
                    MessageId.TORRENT_MESSAGE,
                    FOCMessage("FOC:" + torrentName),
                    true
                )
                send(peer.address, packet)
            }
        }
    }

    override fun sendAppRequest(torrentInfoHash: String, peer: Peer, uuid: String) {
        AppRequestPayload(torrentInfoHash, uuid).let { payload ->
            logger.debug { "-> $payload" }
            send(peer, serializePacket(MessageId.APP_REQUEST, payload))
        }
    }

    init {
        messageHandlers[MessageId.FOC_THALIS_MESSAGE] = ::onMessage
        messageHandlers[MessageId.TORRENT_MESSAGE] = ::onTorrentMessage
        messageHandlers[MessageId.APP_REQUEST] = ::onAppRequestPacket
        messageHandlers[MessageId.APP] = ::onAppPacket
        evaProtocolEnabled = true
    }

    override fun load() {
        super.load()

        setOnEVASendCompleteCallback(::onEVASendCompleteCallback)
        setOnEVAReceiveProgressCallback(::onEVAReceiveProgressCallback)
        setOnEVAReceiveCompleteCallback(::onEVAReceiveCompleteCallback)
        setOnEVAErrorCallback(::onEVAErrorCallback)
    }

    private fun onMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(FOCMessage)
        Log.i("personal", peer.mid + ": " + payload.message)
    }

    private fun onTorrentMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(FOCMessage)
        val torrentHash = payload.message.substringAfter("magnet:?xt=urn:btih:")
            .substringBefore("&dn=")
        if (torrentMessagesList.none {
            it.second
            val existingHash = it.second.message.substringAfter("magnet:?xt=urn:btih:").substringBefore("&dn=")
            torrentHash == existingHash
        }
        ) {
            torrentMessagesList.add(Pair(peer, payload))
            Log.i("personal", peer.mid + ": " + payload.message)
        }
    }

    private fun onAppRequestPacket(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(AppRequestPayload.Deserializer)
        logger.debug { "-> DemoCommunity: Received request $payload from ${peer.mid}" }
        onAppRequest(peer, payload)
    }

    private fun onAppRequest(peer: Peer, appRequestPayload: AppRequestPayload) {
        try {
            locateApp(appRequestPayload.appTorrentInfoHash)?.let { file ->
                logger.debug { "-> sending app ${file.name} to ${peer.mid}" }
                sendApp(peer, appRequestPayload.appTorrentInfoHash, file, appRequestPayload.uuid)
                return
            }
            logger.debug { "Received Request for an app that doesn't exist" }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendApp(peer: Peer, appTorrentInfoHash: String, file: File, uuid: String) {
        val appPayload = AppPayload(appTorrentInfoHash, file.name, file.readBytes())
        val packet =
            serializePacket(MessageId.APP, appPayload, encrypt = true, recipient = peer)
        if (evaProtocolEnabled) evaSendBinary(
            peer,
            EVA_FOC_COMMUNITY_ATTACHMENT,
            uuid,
            packet
        ) else send(peer, packet)
    }

    private fun locateApp(appTorrentInfoHash: String): File? {
        appDirectory.listFiles()?.forEachIndexed { _, file ->
            if (file.name.endsWith(".torrent")) {
                TorrentInfo(file).let { torrentInfo ->
                    if (torrentInfo.infoHash().toString() == appTorrentInfoHash) {
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

    private fun isTorrentOkay(torrentInfo: TorrentInfo, saveDirectory: File): Boolean {
        File(saveDirectory.path + "/" + torrentInfo.name()).run {
            if (!arrayListOf("jar", "apk").contains(extension)) return false
            if (length() >= torrentInfo.totalSize()) return true
        }
        return false
    }

    private fun onAppPacket(packet: Packet) {
        val (peer, payload) = packet.getDecryptedAuthPayload(
            AppPayload.Deserializer, myPeer.key as PrivateKey
        )
        logger.debug { "<- Received app from ${peer.mid}" }
        val file = appDirectory.toString() + "/" + payload.appName
        val existingFile = File(file)
        if (!existingFile.exists()) {
            try {
                val os = FileOutputStream(file)
                os.write(payload.data)
            } catch (e: Exception) {
                logger.debug { "Could not write file from $peer with hash ${payload.appTorrentInfoHash}" }
            }
        } else {
            logger.error { "File $file already exists, will not overwrite after EVA download" }
        }
    }

    private fun onEVASendCompleteCallback(peer: Peer, info: String, nonce: ULong) {
        Log.d("DemoCommunity", "ON EVA send complete callback for '$info'")

        if (info != EVA_FOC_COMMUNITY_ATTACHMENT) return

        if (this::evaSendCompleteCallback.isInitialized) {
            this.evaSendCompleteCallback(peer, info, nonce)
        }
    }

    private fun onEVAReceiveProgressCallback(peer: Peer, info: String, progress: TransferProgress) {
        Log.d("DemoCommunity", "ON EVA receive progress callback for '$info'")

        if (info != EVA_FOC_COMMUNITY_ATTACHMENT) return

        if (this::evaReceiveProgressCallback.isInitialized) {
            this.evaReceiveProgressCallback(peer, info, progress)
        }
    }

    private fun onEVAReceiveCompleteCallback(peer: Peer, info: String, id: String, data: ByteArray?) {
        Log.d("DemoCommunity", "ON EVA receive complete callback for '$info'")

        if (info != EVA_FOC_COMMUNITY_ATTACHMENT) return

        data?.let {
            val packet = Packet(peer.address, it)
            onAppPacket(packet)
        }

        if (this::evaReceiveCompleteCallback.isInitialized) {
            this.evaReceiveCompleteCallback(peer, info, id, data)
        }
    }

    private fun onEVAErrorCallback(peer: Peer, exception: TransferException) {
        Log.d("DemoCommunity", "ON EVA error callback for '${exception.info} from ${peer.mid}'")

        if (this::evaErrorCallback.isInitialized) {
            this.evaErrorCallback(peer, exception)
        }
    }

    class Factory(
        private val context: Context
    ) : Overlay.Factory<FOCCommunity>(FOCCommunity::class.java) {
        override fun create(): FOCCommunity {
            return FOCCommunity(context)
        }
    }

    companion object {
        // Use this until we can commit an id to kotlin ipv8
        const val EVA_FOC_COMMUNITY_ATTACHMENT = "eva_foc_community_attachment"
    }
}
