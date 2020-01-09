package nl.tudelft.ipv8

import android.util.Log
import nl.tudelft.ipv8.messaging.payload.ConnectionType
import nl.tudelft.ipv8.messaging.payload.IntroductionRequestPayload
import nl.tudelft.ipv8.messaging.Endpoint
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.payload.BinMemberAuthenticationPayload
import nl.tudelft.ipv8.messaging.payload.GlobalTimeDistributionPayload
import nl.tudelft.ipv8.peerdiscovery.Network
import java.util.*

abstract class Community(
    myPeer: Peer,
    endpoint: Endpoint,
    network: Network,
    maxPeers: Int = DEFAULT_MAX_PEERS
) : Overlay(myPeer, endpoint, network) {
    abstract val masterPeer: Peer

    private val prefix: String
        get() = "00" + VERSION + masterPeer.mid

    var myEstimatedWan: Address? = null
    val myEstimatedLan: Address? = null

    private var lastBootstrap: Date? = null

    private val messageHandlers = mutableMapOf<Int, (ByteArray) -> Unit>()

    init {
        messageHandlers[MessageId.PUNCTURE_REQUEST] = ::onPunctureRequest
        messageHandlers[MessageId.PUNCTURE] = ::onPuncture
        messageHandlers[MessageId.INTRODUCTION_REQUEST] = ::onIntroductionRequest
        messageHandlers[MessageId.INTRODUCTION_RESPONSE] = ::onIntroductionResponse
    }

    override fun load() {
        super.load()

        network.registerServiceProvider(masterPeer.mid, this)
        network.blacklistMids.add(myPeer.mid)
        network.blacklist.addAll(DEFAULT_ADDRESSES)
    }

    override fun bootstrap() {
        if (Date().time - (lastBootstrap?.time ?: 0L) < BOOTSTRAP_TIMEOUT_MS) return
        lastBootstrap = Date()

        for (socketAddress in DEFAULT_ADDRESSES) {
            walkTo(socketAddress)
        }
    }

    override fun walkTo(address: Address) {
        val packet = createIntroductionRequest(address)
        if (packet != null) {
            endpoint.send(address.toSocketAddress(), packet)
        }
    }

    /**
     * Get a new introduction, or bootstrap if there are no available peers.
     */
    override fun getNewIntroduction(fromPeer: Peer?) {
        var peer = fromPeer

        if (peer == null) {
            val available = getPeers()
            if (available.isNotEmpty()) {
                peer = available.random()
            } else {
                bootstrap()
                return
            }
        }

        val packet = createIntroductionRequest(peer.address)
        if (packet != null) {
            endpoint.send(peer.address.toSocketAddress(), packet)
        }
    }

    override fun getPeerForIntroduction(exclude: Peer?): Peer? {
        val available = getPeers() - exclude
        return if (available.isNotEmpty()) {
            available.random()
        } else {
            null
        }
    }

    override fun getWalkableAddresses(): List<Address> {
        return network.getWalkableAddresses(masterPeer.mid)
    }

    override fun getPeers(): List<Peer> {
        return network.getPeersForService(masterPeer.mid)
    }

    override fun onPacket(packet: Packet) {
        val sourceAddress = packet.source
        val data = packet.data
        // TODO
    }

    private fun createIntroductionRequest(socketAddress: Address): ByteArray? {
        val myEstimatedLan = myEstimatedLan
        val myEstimatedWan = myEstimatedWan

        return if (myEstimatedLan != null && myEstimatedWan != null) {
            val globalTime = claimGlobalTime()
            val payload =
                IntroductionRequestPayload(
                    socketAddress,
                    myEstimatedLan,
                    myEstimatedWan,
                    true,
                    ConnectionType.UNKNOWN,
                    (globalTime % 65536u).toInt()
                )
            val auth = BinMemberAuthenticationPayload(myPeer.publicKey.keyToBin().toString())
            val dist = GlobalTimeDistributionPayload(globalTime)

            serializePacket(prefix, MessageId.INTRODUCTION_REQUEST, listOf(auth, dist, payload))
        } else {
            // TODO: should we send the request even in this case?
            Log.e("Overlay", "estimated LAN or WAN is null")
            null
        }
    }

    private fun createIntroductionResponse(): ByteArray? {
        // TODO
        return null
    }

    private fun createPuncture(): ByteArray? {
        // TODO
        return null
    }

    private fun createPunctureRequest(): ByteArray? {
        // TODO
        return null
    }

    private fun onIntroductionRequest(bytes: ByteArray) {
        val payload = IntroductionRequestPayload.deserialize(bytes)
        // TODO: handle
    }

    private fun onIntroductionResponse(bytes: ByteArray) {
        // TODO
    }

    private fun onPuncture(bytes: ByteArray) {
        // TODO
    }

    private fun onPunctureRequest(bytes: ByteArray) {
        // TODO
    }

    private fun serializePacket(prefix: String, messageId: Int, payload: List<Serializable>): ByteArray {
        var bytes = prefix.toByteArray(Charsets.US_ASCII) + messageId.toChar().toByte()
        for (item in payload) {
            bytes += item.serialize()
        }
        return bytes
    }

    companion object {
        private val DEFAULT_ADDRESSES = listOf(
            // Dispersy
            Address("130.161.119.206", 6421),
            Address("130.161.119.206", 6422),
            Address("131.180.27.155", 6423),
            Address("131.180.27.156", 6424),
            Address("131.180.27.161", 6427),
            // IPv8
            Address("131.180.27.161", 6521),
            Address("131.180.27.161", 6522),
            Address("131.180.27.162", 6523),
            Address("131.180.27.162", 6524),
            Address("130.161.119.215", 6525),
            Address("130.161.119.215", 6526),
            Address("81.171.27.194", 6527),
            Address("81.171.27.194", 6528)
        )

        // Timeout before we bootstrap again (bootstrap kills performance)
        private const val BOOTSTRAP_TIMEOUT_MS = 30_000
        private const val DEFAULT_MAX_PEERS = 30

        private const val VERSION = "02"
    }

    object MessageId {
        const val PUNCTURE_REQUEST = 250
        const val PUNCTURE = 249
        const val INTRODUCTION_REQUEST = 246
        const val INTRODUCTION_RESPONSE = 245
    }
}
