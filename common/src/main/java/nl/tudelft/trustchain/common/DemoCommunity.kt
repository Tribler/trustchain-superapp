package nl.tudelft.trustchain.common

import android.content.Context
import android.util.Log
import com.frostwire.jlibtorrent.TorrentInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import mu.KotlinLogging
import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.IPv4Address
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.messaging.Address
import nl.tudelft.ipv8.messaging.Deserializable
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.eva.TransferException
import nl.tudelft.ipv8.messaging.eva.TransferProgress
import nl.tudelft.ipv8.messaging.payload.IntroductionResponsePayload
import nl.tudelft.ipv8.messaging.payload.PuncturePayload
import nl.tudelft.trustchain.common.freedomOfComputing.AppPayload
import nl.tudelft.trustchain.common.freedomOfComputing.AppRequestPayload
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.util.*

private val logger = KotlinLogging.logger {}

@Suppress("deprecation")
@OptIn(ExperimentalUnsignedTypes::class)
class DemoCommunity(
    context: Context
) : Community() {
    override val serviceId = "02313685c1912a141279f8248fc8db5899c5df5a"

    val discoveredAddressesContacted: MutableMap<IPv4Address, Date> = mutableMapOf()

    val lastTrackerResponses = mutableMapOf<IPv4Address, Date>()

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
    private lateinit var evaAppRequestCallback: (
        info: String
    ) -> Unit

    fun setEVAOnSendCompleteCallback(
        f: (peer: Peer, info: String, nonce: ULong) -> Unit
    ) {
        this.evaSendCompleteCallback = f
    }

    fun setEVAOnReceiveProgressCallback(
        f: (peer: Peer, info: String, progress: TransferProgress) -> Unit
    ) {
        this.evaReceiveProgressCallback = f
    }

    fun setEVAOnReceiveCompleteCallback(
        f: (peer: Peer, info: String, id: String, data: ByteArray?) -> Unit
    ) {
        this.evaReceiveCompleteCallback = f
    }

    fun setEVAOnAppRequestCallback(
        f: (info: String) -> Unit
    ) {
        this.evaAppRequestCallback = f
    }

    fun setEVAOnErrorCallback(
        f: (peer: Peer, exception: TransferException) -> Unit
    ) {
        this.evaErrorCallback = f
    }

    @ExperimentalCoroutinesApi
    val punctureChannel = BroadcastChannel<Pair<Address, PuncturePayload>>(10000)

    // Retrieve the trustchain community
    private fun getTrustChainCommunity(): TrustChainCommunity {
        return IPv8Android.getInstance().getOverlay()
            ?: throw IllegalStateException("TrustChainCommunity is not configured")
    }

    override fun walkTo(address: IPv4Address) {
        super.walkTo(address)

        discoveredAddressesContacted[address] = Date()
    }

    override fun onIntroductionResponse(peer: Peer, payload: IntroductionResponsePayload) {
        super.onIntroductionResponse(peer, payload)

        if (peer.address in DEFAULT_ADDRESSES) {
            lastTrackerResponses[peer.address] = Date()
        }
    }

    private var torrentMessagesList = ArrayList<Packet>()

    public fun getTorrentMessages(): ArrayList<Packet> {
        return torrentMessagesList
    }

    object MessageId {
        const val THALIS_MESSAGE = 222
        const val TORRENT_MESSAGE = 223
        const val APP_REQUEST = 224
        const val APP = 225
        const val PUNCTURE_TEST = 251
    }

    // SEND MESSAGE
    fun broadcastGreeting() {
        for (peer in getPeers()) {
            val packet = serializePacket(
                MessageId.THALIS_MESSAGE,
                MyMessage("Hello from Freedom of Computing!"),
                true
            )
            send(peer.address, packet)
        }
    }

    fun informAboutTorrent(torrentName: String) {
        if (torrentName != "") {
            for (peer in getPeers()) {
                val packet = serializePacket(
                    MessageId.TORRENT_MESSAGE,
                    MyMessage("FOC:" + torrentName),
                    true
                )
                send(peer.address, packet)
            }
        }
    }

    fun sendPuncture(address: IPv4Address, id: Int) {
        val payload = PuncturePayload(myEstimatedLan, myEstimatedWan, id)
        val packet = serializePacket(MessageId.PUNCTURE_TEST, payload, sign = false)
        endpoint.send(address, packet)
    }

    fun sendAppRequest(torrentInfoHash: String, peer: Peer) {
        AppRequestPayload(torrentInfoHash).let { payload ->
            logger.debug { "-> $payload" }
            send(peer, serializePacket(MessageId.APP_REQUEST, payload))
        }
    }

    // RECEIVE MESSAGE
    init {
        messageHandlers[MessageId.THALIS_MESSAGE] = ::onMessage
        messageHandlers[MessageId.TORRENT_MESSAGE] = ::onTorrentMessage
        messageHandlers[MessageId.APP_REQUEST] = ::onAppRequestPacket
        messageHandlers[MessageId.APP] = ::onAppPacket
        messageHandlers[MessageId.PUNCTURE_TEST] = ::onPunctureTest

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
        val (peer, payload) = packet.getAuthPayload(MyMessage.Deserializer)
        Log.i("personal", peer.mid + ": " + payload.message)
    }

    private fun onTorrentMessage(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(MyMessage.Deserializer)
        val torrentHash = payload.message.substringAfter("magnet:?xt=urn:btih:")
            .substringBefore("&dn=")
        if (torrentMessagesList.none {
                val (_, existingPayload) = it.getAuthPayload(MyMessage.Deserializer)
                val existingHash = existingPayload.message.substringAfter("magnet:?xt=urn:btih:")
                    .substringBefore("&dn=")
                torrentHash == existingHash
            }
        ) {
            torrentMessagesList.add(packet)
            Log.i("personal", peer.mid + ": " + payload.message)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun onPunctureTest(packet: Packet) {
        val payload = packet.getPayload(PuncturePayload.Deserializer)
        punctureChannel.offer(Pair(packet.source, payload))
    }

    private fun onAppRequestPacket(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(AppRequestPayload.Deserializer)
        logger.debug { "-> DemoCommunity: Received request $payload from ${peer.mid}" }
        onAppRequest(peer, payload.appTorrentInfoHash)
    }

    private fun onAppRequest(peer: Peer, appTorrentInfoHash: String) {
        try {
            locateApp(appTorrentInfoHash)?.let { file ->
                logger.debug { "-> sending app ${file.name} to ${peer.mid}" }
                sendApp(peer, appTorrentInfoHash, file)
                evaAppRequestCallback("sent")
                return
            }
            logger.debug { "Received Request for an app that doesn't exist" }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendApp(peer: Peer, appTorrentInfoHash: String, file: File) {
        val appPayload = AppPayload(appTorrentInfoHash, file.name, file.readBytes())
        val packet =
            serializePacket(MessageId.APP, appPayload, encrypt = true, recipient = peer, timestamp = System.currentTimeMillis().toULong())
        if (evaProtocolEnabled) evaSendBinary(
            peer,
            EVA_DEMOCOMMUNITY_ATTACHMENT,
            appTorrentInfoHash,
            packet,
            System.currentTimeMillis()
        )
        else send(peer, packet)
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
        evaAppRequestCallback("RECEIVED")
        val (peer, payload) = packet.getDecryptedAuthPayload(
            AppPayload.Deserializer, myPeer.key as PrivateKey
        )
        logger.debug { "<- Received app from ${peer.mid}" }
        val file = appDirectory.toString() + "/" + payload.appName
        val existingFile = File(file)
        if(!existingFile.exists()) {
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

        if (info != EVA_DEMOCOMMUNITY_ATTACHMENT) return

        if (this::evaSendCompleteCallback.isInitialized) {
            this.evaSendCompleteCallback(peer, info, nonce)
        }
    }

    private fun onEVAReceiveProgressCallback(peer: Peer, info: String, progress: TransferProgress) {
        Log.d("DemoCommunity", "ON EVA receive progress callback for '$info'")

        if (info != EVA_DEMOCOMMUNITY_ATTACHMENT) return

        if (this::evaReceiveProgressCallback.isInitialized) {
            this.evaReceiveProgressCallback(peer, info, progress)
        }
    }

    private fun onEVAReceiveCompleteCallback(peer: Peer, info: String, id: String, data: ByteArray?) {
        Log.d("DemoCommunity", "ON EVA receive complete callback for '$info'")

        if (info != EVA_DEMOCOMMUNITY_ATTACHMENT) return

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
    ) : Overlay.Factory<DemoCommunity>(DemoCommunity::class.java) {
        override fun create(): DemoCommunity {
            return DemoCommunity(context)
        }
    }

    companion object {
        //Use this until we can commit an id to kotlin ipv8
        const val EVA_DEMOCOMMUNITY_ATTACHMENT = "eva_democommunity_attachment"
    }
}

// THE MESSAGE (CLASS)
data class MyMessage(val message: String) : nl.tudelft.ipv8.messaging.Serializable {
    override fun serialize(): ByteArray {
        return message.toByteArray()
    }

    companion object Deserializer : Deserializable<MyMessage> {
        override fun deserialize(buffer: ByteArray, offset: Int): Pair<MyMessage, Int> {
            var toReturn = buffer.toString(Charsets.UTF_8)
            return Pair(MyMessage(toReturn), buffer.size)
        }
    }
}
