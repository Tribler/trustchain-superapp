package nl.tudelft.ipv8

import kotlinx.coroutines.*
import mu.KotlinLogging
import nl.tudelft.ipv8.keyvault.CryptoProvider
import nl.tudelft.ipv8.keyvault.JavaCryptoProvider
import nl.tudelft.ipv8.keyvault.PrivateKey
import nl.tudelft.ipv8.messaging.Endpoint
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.Serializable
import nl.tudelft.ipv8.messaging.payload.*
import nl.tudelft.ipv8.peerdiscovery.Network
import nl.tudelft.ipv8.util.addressIsLan
import nl.tudelft.ipv8.util.hexToBytes
import java.util.*
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

abstract class Community : Overlay {
    protected val prefix: ByteArray
        get() = ByteArray(0) + 0.toByte() + VERSION + serviceId.hexToBytes()

    override var myEstimatedWan: Address = Address.EMPTY
    override var myEstimatedLan: Address = Address.EMPTY

    private var lastBootstrap: Date? = null

    val messageHandlers = mutableMapOf<Int, (Packet) -> Unit>()

    override lateinit var myPeer: Peer
    override lateinit var endpoint: Endpoint
    override lateinit var network: Network
    override var maxPeers: Int = 20
    override var cryptoProvider: CryptoProvider = JavaCryptoProvider

    private lateinit var job: Job
    protected lateinit var scope: CoroutineScope

    init {
        messageHandlers[MessageId.PUNCTURE_REQUEST] = ::onPunctureRequestPacket
        messageHandlers[MessageId.PUNCTURE] = ::onPuncturePacket
        messageHandlers[MessageId.INTRODUCTION_REQUEST] = ::onIntroductionRequestPacket
        messageHandlers[MessageId.INTRODUCTION_RESPONSE] = ::onIntroductionResponsePacket
    }

    override fun load() {
        super.load()

        logger.info { "Loading " + javaClass.simpleName + " for peer " + myPeer.mid }

        network.registerServiceProvider(serviceId, this)
        network.blacklistMids.add(myPeer.mid)
        network.blacklist.addAll(DEFAULT_ADDRESSES)

        job = SupervisorJob()
        scope = CoroutineScope(Dispatchers.Main + job)
    }

    override fun unload() {
        super.unload()

        job.cancel()
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
        send(address, packet)
    }

    /**
     * Get a new introduction, or bootstrap if there are no available peers.
     */
    override fun getNewIntroduction(fromPeer: Peer?) {
        var address = fromPeer?.address

        if (address == null) {
            val available = getPeers()
            address = if (available.isNotEmpty()) {
                // With a small chance, try to remedy any disconnected network phenomena.
                if (Random.nextFloat() < 0.5f) {
                    DEFAULT_ADDRESSES.random()
                } else {
                    available.random().address
                }
            } else {
                bootstrap()
                return
            }
        }

        val packet = createIntroductionRequest(address)
        send(address, packet)
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
        return network.getWalkableAddresses(serviceId)
    }

    override fun getPeers(): List<Peer> {
        return network.getPeersForService(serviceId)
    }

    override fun onPacket(packet: Packet) {
        val sourceAddress = packet.source
        val data = packet.data

        val probablePeer = network.getVerifiedByAddress(sourceAddress)
        if (probablePeer != null) {
            probablePeer.lastResponse = Date()
        }

        val packetPrefix = data.copyOfRange(0, prefix.size)
        if (!packetPrefix.contentEquals(prefix)) {
            // logger.debug("prefix not matching")
            return
        }

        val msgId = data[prefix.size].toUByte().toInt()
        val handler = messageHandlers[msgId]

        if (handler != null) {
            try {
                handler(packet)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            logger.debug("Received unknown message $msgId from $sourceAddress")
        }
    }

    override fun onEstimatedLanChanged(address: Address) {
        myEstimatedLan = address
    }

    /*
     * Introduction and puncturing requests creation
     */

    internal fun createIntroductionRequest(socketAddress: Address): ByteArray {
        val globalTime = claimGlobalTime()
        val payload = IntroductionRequestPayload(
            socketAddress,
            myEstimatedLan,
            myEstimatedWan,
            true,
            ConnectionType.UNKNOWN,
            (globalTime % UShort.MAX_VALUE).toInt()
        )

        logger.debug("-> $payload")

        return serializePacket(MessageId.INTRODUCTION_REQUEST, payload)
    }

    fun createIntroductionResponse(
        requester: Peer,
        identifier: Int,
        introduction: Peer? = null,
        prefix: ByteArray = this.prefix
    ): ByteArray {
        val intro = introduction ?: getPeerForIntroduction(exclude = requester)
        val introductionLan = intro?.lanAddress ?: Address.EMPTY
        val introductionWan = intro?.wanAddress ?: Address.EMPTY

        val payload = IntroductionResponsePayload(
            requester.address,
            myEstimatedLan,
            myEstimatedWan,
            introductionLan,
            introductionWan,
            ConnectionType.UNKNOWN,
            false,
            identifier
        )

        if (intro != null) {
            // TODO: Seems like a bad practice to send a packet in the create method...
            val packet = createPunctureRequest(requester.lanAddress, requester.wanAddress,
                identifier)
            send(intro.address, packet)
        }

        logger.debug("-> $payload")

        return serializePacket(MessageId.INTRODUCTION_RESPONSE, payload, prefix = prefix)
    }

    internal fun createPuncture(lanWalker: Address, wanWalker: Address, identifier: Int): ByteArray {
        val payload = PuncturePayload(lanWalker, wanWalker, identifier)

        logger.debug("-> $payload")

        return serializePacket(MessageId.PUNCTURE, payload)
    }

    internal fun createPunctureRequest(lanWalker: Address, wanWalker: Address, identifier: Int): ByteArray {
        logger.debug("-> punctureRequest")
        val payload = PunctureRequestPayload(lanWalker, wanWalker, identifier)
        return serializePacket(MessageId.PUNCTURE_REQUEST, payload, sign = false)
    }

    /**
     * Serializes a payload into a binary packet that can be sent over the transport.
     *
     * @param messageId The message type ID
     * @param payload The serializable payload
     * @param sign True if the packet should be signed
     * @param peer The peer that should sign the packet. The community's [myPeer] is used by default.
     */
    protected fun serializePacket(
        messageId: Int,
        payload: Serializable,
        sign: Boolean = true,
        peer: Peer = myPeer,
        prefix: ByteArray = this.prefix
    ): ByteArray {
        val payloads = mutableListOf<Serializable>()
        if (sign) {
            payloads += BinMemberAuthenticationPayload(peer.publicKey.keyToBin())
        }
        payloads += GlobalTimeDistributionPayload(claimGlobalTime())
        payloads += payload
        return serializePacket(
            messageId,
            payloads,
            sign,
            peer,
            prefix
        )
    }

    /**
     * Serializes multiple payloads into a binary packet that can be sent over the transport.
     *
     * @param messageId The message type ID
     * @param payload The list of payloads
     * @param sign True if the packet should be signed
     * @param peer The peer that should sign the packet. The community's [myPeer] is used by default.
     */
    private fun serializePacket(
        messageId: Int,
        payload: List<Serializable>,
        sign: Boolean = true,
        peer: Peer = myPeer,
        prefix: ByteArray = this.prefix
    ): ByteArray {
        var packet = prefix
        packet += messageId.toChar().toByte()

        for (item in payload) {
            packet += item.serialize()
        }

        val myPeerKey = peer.key
        if (sign && myPeerKey is PrivateKey) {
            packet += myPeerKey.sign(packet)
        }

        return packet
    }

    /*
     * Request deserialization
     */

    internal fun onIntroductionRequestPacket(packet: Packet) {
        val (peer, payload) =
            packet.getAuthPayload(IntroductionRequestPayload.Deserializer)
        onIntroductionRequest(peer, payload)
    }

    internal fun onIntroductionResponsePacket(packet: Packet) {
        val (peer, payload) =
            packet.getAuthPayload(IntroductionResponsePayload.Deserializer)
        onIntroductionResponse(peer, payload)
    }

    internal fun onPuncturePacket(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(PuncturePayload.Deserializer)
        onPuncture(peer, payload)
    }

    internal fun onPunctureRequestPacket(packet: Packet) {
        val payload = packet.getPayload(PunctureRequestPayload.Deserializer)
        onPunctureRequest(packet.source, payload)
    }

    /*
     * Request handling
     */

    internal open fun onIntroductionRequest(
        peer: Peer,
        payload: IntroductionRequestPayload
    ) {
        logger.debug("<- $payload")

        if (maxPeers >= 0 && getPeers().size >= maxPeers) {
            logger.info("Dropping introduction request from $peer, too many peers!")
            return
        }

        // Add the sender as a verified peer
        val newPeer = peer.copy(
            lanAddress = payload.sourceLanAddress,
            wanAddress = payload.sourceWanAddress
        )
        addVerifiedPeer(newPeer)

        val packet = createIntroductionResponse(
            newPeer,
            payload.identifier
        )

        send(peer.address, packet)
    }

    open fun onIntroductionResponse(
        peer: Peer,
        payload: IntroductionResponsePayload
    ) {
        logger.debug("<- $payload")

        // Change our estimated WAN address if the sender is not on the same LAN, otherwise it
        // would just send us our LAN address
        if (!addressIsLan(peer.address)) {
            myEstimatedWan = payload.destinationAddress
        }

        // Add the sender as a verified peer
        val newPeer = peer.copy(
            lanAddress = payload.sourceLanAddress,
            wanAddress = payload.sourceWanAddress
        )
        addVerifiedPeer(newPeer)

        // Process introduced addresses
        if (!payload.wanIntroductionAddress.isEmpty() &&
            payload.wanIntroductionAddress.ip != myEstimatedWan.ip) {
            // WAN is not empty and it is not same as ours

            if (!payload.lanIntroductionAddress.isEmpty()) {
                // If LAN address is not empty, add them in case they are on our LAN,
                // even though that should not happen as WAN is different than ours
                discoverAddress(peer, payload.lanIntroductionAddress, serviceId)
            }

            // Discover WAN address. We should contact it ASAP as it just received a puncture
            // request and probably already sent a puncture to us.
            discoverAddress(peer, payload.wanIntroductionAddress, serviceId)
        } else if (!payload.lanIntroductionAddress.isEmpty() &&
            payload.wanIntroductionAddress.ip == myEstimatedWan.ip) {
            // LAN is not empty and WAN is the same as ours => they are on the same LAN
            discoverAddress(peer, payload.lanIntroductionAddress, serviceId)
        } else if (!payload.wanIntroductionAddress.isEmpty()) {
            // WAN is same as ours, but we do not know the LAN
            // Should not happen with kotlin-ipv8 peers, but we'll keep it for py-ipv8 compatibility

            // Try to connect via WAN, NAT needs to support hairpinning
            discoverAddress(peer, payload.wanIntroductionAddress, serviceId)

            // Assume LAN is same as ours (e.g. multiple instances running on a local machine),
            // and port same as for WAN (works only if NAT does not change port)
            discoverAddress(peer, Address(myEstimatedLan.ip, payload.wanIntroductionAddress.port),
                serviceId)
        }
    }

    protected fun addVerifiedPeer(peer: Peer) {
        network.addVerifiedPeer(peer)
        network.discoverServices(peer, listOf(serviceId))
    }

    protected open fun discoverAddress(peer: Peer, address: Address, serviceId: String) {
        // Prevent discovering its own address
        if (address != myEstimatedLan && address != myEstimatedWan && !address.isEmpty()) {
            network.discoverAddress(peer, address, serviceId)
        }
    }

    internal open fun onPuncture(
        peer: Peer,
        payload: PuncturePayload
    ) {
        logger.debug("<- $payload")
        // NOOP
    }

    internal open fun onPunctureRequest(
        address: Address,
        payload: PunctureRequestPayload
    ) {
        logger.debug("<- $payload")

        val target = if (payload.wanWalkerAddress.ip == myEstimatedWan.ip) {
            // They are on the same LAN, puncture should not be needed, but send it just in case
            payload.lanWalkerAddress
        } else {
            payload.wanWalkerAddress
        }

        val packet = createPuncture(myEstimatedLan, myEstimatedWan, payload.identifier)
        send(target, packet)
    }

    protected fun send(address: Address, data: ByteArray) {
        val probablePeer = network.getVerifiedByAddress(address)
        if (probablePeer != null) {
            probablePeer.lastRequest = Date()
        }
        endpoint.send(address, data)
    }

    companion object {
        val DEFAULT_ADDRESSES = listOf<Address>(
            // Dispersy
            // Address("130.161.119.206", 6421),
            // Address("130.161.119.206", 6422),
            // Address("131.180.27.155", 6423),
            // Address("131.180.27.156", 6424),
            // Address("131.180.27.161", 6427),
            // IPv8
            // Address("131.180.27.161", 6521),
            // Address("131.180.27.161", 6522),
            // Address("131.180.27.162", 6523),
            // Address("131.180.27.162", 6524),
            // Address("130.161.119.215", 6525),
            // Address("130.161.119.215", 6526),
            // Address("81.171.27.194", 6527),
            // Address("81.171.27.194", 6528)
            // py-ipv8 + LibNaCL
            Address("131.180.27.161", 6427),
            // kotlin-ipv8
            Address("131.180.27.188", 1337)
        )

        // Timeout before we bootstrap again (bootstrap kills performance)
        private const val BOOTSTRAP_TIMEOUT_MS = 5_000
        const val DEFAULT_MAX_PEERS = 30

        private const val VERSION: Byte = 2
    }

    object MessageId {
        const val PUNCTURE_REQUEST = 250
        const val PUNCTURE = 249
        const val INTRODUCTION_REQUEST = 246
        const val INTRODUCTION_RESPONSE = 245
    }
}
