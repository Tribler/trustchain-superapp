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
    network: Network
) : Overlay(myPeer, endpoint, network) {
    abstract val masterPeer: Peer

    private val prefix: String
        get() = "00" + VERSION + masterPeer.mid

    var myEstimatedWan: Address? = null
    val myEstimatedLan: Address? = null

    private var lastBootstrap: Date? = null

    private val messageHandlers = mutableMapOf<Int, (ByteArray) -> Unit>()

    init {
        messageHandlers[MessageId.INTRODUCTION_REQUEST] = ::handleIntroductionRequest
    }

    override fun onPacket(packet: Packet) {
        val sourceAddress = packet.source
        val data = packet.data
        // TODO
    }

    /**
     * Perform introduction logic to get into the network.
     */
    fun bootstrap() {
        if (Date().time - (lastBootstrap?.time ?: 0L) < BOOTSTRAP_TIMEOUT_MS) return
        lastBootstrap = Date()

        for (socketAddress in DEFAULT_ADDRESSES) {
            walkTo(socketAddress)
        }
    }

    fun createIntroductionRequest(socketAddress: Address): ByteArray? {
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
            val auth = BinMemberAuthenticationPayload(myPeer.publicKey.keyToBin())
            val dist = GlobalTimeDistributionPayload(globalTime)

            serializePacket(prefix, MessageId.INTRODUCTION_REQUEST, listOf(auth, dist, payload))
        } else {
            // TODO: should we send the request even in this case?
            Log.e("Overlay", "estimated LAN or WAN is null")
            null
        }
    }

    fun handleIntroductionRequest(bytes: ByteArray) {
        val payload = IntroductionRequestPayload.deserialize(bytes)
        // TODO: handle
    }

    /**
     * Puncture the NAT of an address.
     *
     * @param address The address to walk to.
     */
    fun walkTo(address: Address) {
        val packet = createIntroductionRequest(address)
        if (packet != null) {
            endpoint.send(address.toSocketAddress(), packet)
        }
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
        private val BOOTSTRAP_TIMEOUT_MS = 30_000
        private val DEFAULT_MAX_PEERS = 30

        private val VERSION = "02"
    }

    object MessageId {
        val PUNCTURE_REQUEST = 250
        val PUNCTURE = 249
        val INTRODUCTION_REQUEST = 246
        val INTRODUCTION_RESPONSE = 245
    }
}
