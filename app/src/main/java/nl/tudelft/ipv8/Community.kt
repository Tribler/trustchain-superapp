package nl.tudelft.ipv8

import android.util.Log
import nl.tudelft.ipv8.exception.PacketDecodingException
import nl.tudelft.ipv8.keyvault.LibNaClPK
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.messaging.Endpoint
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.payload.*
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

    var myEstimatedWan: Address = Address("0.0.0.0", 0)
    var myEstimatedLan: Address = Address("0.0.0.0", 0)

    private var lastBootstrap: Date? = null

    private val messageHandlers = mutableMapOf<Int, (Address, ByteArray) -> Unit>()

    init {
        //messageHandlers[MessageId.PUNCTURE_REQUEST] = ::handlePunctureRequest
        //messageHandlers[MessageId.PUNCTURE] = ::handlePuncture
        messageHandlers[MessageId.INTRODUCTION_REQUEST] = ::handleIntroductionRequest
        //messageHandlers[MessageId.INTRODUCTION_RESPONSE] = ::handleIntroductionResponse
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
        endpoint.send(address.toSocketAddress(), packet)
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
        endpoint.send(peer.address.toSocketAddress(), packet)
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

        val packetPrefix = data.copyOfRange(0, prefix.length).toString(Charsets.US_ASCII)
        if (packetPrefix != prefix) return

        val msgId = data.copyOfRange(22, 23).first().toChar().toInt()
        val handler = messageHandlers[msgId]

        if (handler != null) {
            val payload = data.copyOfRange(23, data.size)
            try {
                handler(sourceAddress, payload)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            Log.d(TAG, "Received unknown message $msgId from $sourceAddress")
        }
    }

    /*
     * Introduction and puncturing requests creation
     */

    internal fun createIntroductionRequest(socketAddress: Address): ByteArray {
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

        return serializePacket(prefix, MessageId.INTRODUCTION_REQUEST, listOf(auth, dist, payload))
    }

    internal fun createIntroductionResponse(
        lanSocketAddress: Address,
        socketAddress: Address,
        identifier: Int,
        introduction: Peer? = null
    ): ByteArray {
        val globalTime = claimGlobalTime()
        var introductionLan = Address("0.0.0.0", 0)
        var introductionWan = Address("0.0.0.0", 0)
        var introduced = false
        val other = network.getVerifiedByAddress(socketAddress)
        var intro = introduction
        if (intro == null) {
            intro = getPeerForIntroduction(exclude = other)
        }
        if (intro != null) {
            // TODO: understand how this works
            if (addressIsLan(intro.address)) {
                introductionLan = intro.address
                introductionWan = Address(myEstimatedWan.ip, introductionLan.port)
            } else {
                introductionWan = intro.address
            }
            introduced = true
        }
        val payload = IntroductionResponsePayload(
            socketAddress,
            myEstimatedLan,
            myEstimatedWan,
            introductionLan,
            introductionWan,
            ConnectionType.UNKNOWN,
            false,
            identifier
        )
        val auth = BinMemberAuthenticationPayload(myPeer.publicKey.keyToBin())
        val dist = GlobalTimeDistributionPayload(globalTime)

        if (introduced) {
            // TODO: Seems like a bad practice to send a packet in the create method...
            //val packet = createPunctureRequest()
            //endpoint.send()
        }

        return serializePacket(prefix, MessageId.INTRODUCTION_RESPONSE, listOf(auth, dist, payload))
    }

    private fun createPuncture(): ByteArray {
        // TODO
        return ByteArray(0)
    }

    private fun createPunctureRequest(): ByteArray {
        // TODO
        return ByteArray(0)
    }

    /**
     * Serializes multiple payloads into a binary packet that can be sent over the transport.
     *
     * @param prefix The packet prefix consisting of a zero byte, version, and master peer mid
     * @param messageId The message type ID
     * @param payload The list of payloads
     * @param sign True if the packet should be signed
     */
    protected fun serializePacket(
        prefix: String,
        messageId: Int,
        payload: List<Serializable>,
        sign: Boolean = true
    ): ByteArray {
        var packet = prefix.toByteArray(Charsets.US_ASCII) + messageId.toChar().toByte()

        for (item in payload) {
            packet += item.serialize()
        }

        if (sign && myPeer.key is PrivateKey) {
            packet += myPeer.key.sign(packet)
        }

        return packet
    }

    /*
     * Request deserialization
     */

    internal fun handleIntroductionRequest(address: Address, bytes: ByteArray) {
        val (peer, remainder) = unwrapAuthPacket(address, bytes)
        val (dist, distSize) = GlobalTimeDistributionPayload.deserialize(remainder)
        val (payload, _) = IntroductionRequestPayload.deserialize(remainder, distSize)
        onIntroductionRequest(peer, dist, payload)
    }

    /**
     * Checks the signature of the authenticated packet and returns the packet payloads excluding
     * BinMemberAuthenticationPayload if it is valid.
     *
     * @throws PacketDecodingException if the signature is invalid
     */
    internal fun unwrapAuthPacket(address: Address, bytes: ByteArray): Pair<Peer, ByteArray> {
        val (auth, authSize) = BinMemberAuthenticationPayload.deserialize(bytes, 0)
        val publicKey = LibNaClPK.fromBin(auth.publicKey)
        val signature = bytes.copyOfRange(bytes.size - publicKey.getSignatureLength(), bytes.size)

        // Verify signature
        val isValidSignature = publicKey.verify(signature, bytes)
        if (!isValidSignature)
            throw PacketDecodingException("Incoming packet has an invalid signature")

        // Return the peer and remaining payloads
        val peer = Peer(LibNaClPK.fromBin(auth.publicKey), address)
        val remainder = bytes.copyOfRange(authSize, bytes.size - publicKey.getSignatureLength())
        return Pair(peer, remainder)
    }

    /*
     * Request handling
     */

    private fun onIntroductionRequest(
        peer: Peer,
        dist: GlobalTimeDistributionPayload,
        payload: IntroductionRequestPayload
    ) {
        network.addVerifiedPeer(peer)
        network.discoverServices(peer, listOf(masterPeer.mid))

        val packet = createIntroductionResponse(
            payload.destinationAddress,
            peer.address,
            payload.identifier
        )

        endpoint.send(peer.address.toSocketAddress(), packet)
    }

    private fun onIntroductionResponse(address: Address, bytes: ByteArray) {
        // TODO
    }

    private fun onPuncture(address: Address, bytes: ByteArray) {
        // TODO
    }

    private fun onPunctureRequest(address: Address, bytes: ByteArray) {
        // TODO
    }

    private fun addressIsLan(address: Address): Boolean {
        // TODO
        return false
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

        private const val TAG = "Community"
    }

    object MessageId {
        const val PUNCTURE_REQUEST = 250
        const val PUNCTURE = 249
        const val INTRODUCTION_REQUEST = 246
        const val INTRODUCTION_RESPONSE = 245
    }
}
