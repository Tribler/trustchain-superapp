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

    var myEstimatedWan: Address = Address.EMPTY
    var myEstimatedLan: Address = Address.EMPTY

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

        val msgId = data.copyOfRange(prefix.size, prefix.size + 1).first()
        val handler = messageHandlers[msgId.toUByte().toInt()]

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
        val payload =
            IntroductionRequestPayload(
                socketAddress,
                myEstimatedLan,
                myEstimatedWan,
                true,
                ConnectionType.UNKNOWN,
                (globalTime % 65536u).toInt()
            )

        logger.debug("-> $payload")

        return serializePacket(MessageId.INTRODUCTION_REQUEST, payload)
    }

    internal fun createIntroductionResponse(
        lanSocketAddress: Address,
        socketAddress: Address,
        identifier: Int
    ): ByteArray {
        var introductionLan = Address.EMPTY
        var introductionWan = Address.EMPTY
        val other = network.getVerifiedByAddress(socketAddress)
        val intro = getPeerForIntroduction(exclude = other)
        if (intro != null) {
            /*
             * If we are introducting a peer on our LAN, we assume our WAN is same as their WAN,
             * and use their LAN port as a WAN port. Note that this will only work if the port is
             * not translated by NAT.
             */
            if (addressIsLan(intro.address)) {
                introductionLan = intro.address
                introductionWan = Address(myEstimatedWan.ip, introductionLan.port)
            } else {
                introductionWan = intro.address
            }
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

        if (intro != null) {
            // TODO: Seems like a bad practice to send a packet in the create method...
            val packet = createPunctureRequest(lanSocketAddress, socketAddress, identifier)
            val punctureRequestAddress = if (introductionLan.isEmpty())
                introductionWan else introductionLan
            send(punctureRequestAddress, packet)
        }

        logger.debug("-> $payload")

        return serializePacket(MessageId.INTRODUCTION_RESPONSE, payload)
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
        peer: Peer = myPeer
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
            peer
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
        peer: Peer = myPeer
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

        network.addVerifiedPeer(peer)
        network.discoverServices(peer, listOf(serviceId))

        val packet = createIntroductionResponse(
            payload.sourceLanAddress,
            payload.sourceWanAddress,
            payload.identifier
        )

        send(peer.address, packet)
    }

    open fun onIntroductionResponse(
        peer: Peer,
        payload: IntroductionResponsePayload
    ) {
        logger.debug("<- $payload")

        // Accept a new WAN address if the sender is not on the same LAN, otherwise they would
        // just send us our LAN address
        if (payload.sourceWanAddress.ip != myEstimatedWan.ip) {
            myEstimatedWan = payload.destinationAddress
        }

        network.addVerifiedPeer(peer)
        network.discoverServices(peer, listOf(serviceId))

        if (!payload.wanIntroductionAddress.isEmpty() &&
            payload.wanIntroductionAddress.ip != myEstimatedWan.ip) {
            // WAN is not empty and it is not the same as ours
            if (!payload.lanIntroductionAddress.isEmpty()) {
                // If LAN address is not empty, add them in case they are on our LAN
                discoverAddress(peer, payload.lanIntroductionAddress, serviceId)
            }
            discoverAddress(peer, payload.wanIntroductionAddress, serviceId)
        } else if (!payload.lanIntroductionAddress.isEmpty() &&
            payload.wanIntroductionAddress.ip == myEstimatedWan.ip) {
            // LAN is not empty and WAN is the same as ours => they are on the same LAN
            discoverAddress(peer, payload.lanIntroductionAddress, serviceId)
        } else if (!payload.wanIntroductionAddress.isEmpty()) {
            // WAN is the same as ours, but we do not know the LAN => we assume LAN is the same as ours
            discoverAddress(peer, payload.wanIntroductionAddress, serviceId)
            discoverAddress(peer, Address(myEstimatedLan.ip, payload.wanIntroductionAddress.port), serviceId)
        }
    }

    protected open fun discoverAddress(peer: Peer, address: Address, serviceId: String) {
        // Prevent discovering its own address
        if (address != myEstimatedLan && address != myEstimatedWan) {
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
        var target = payload.wanWalkerAddress
        if (payload.wanWalkerAddress.ip == myEstimatedWan.ip) {
            target = payload.lanWalkerAddress
        }

        val packet = createPuncture(myEstimatedLan, payload.wanWalkerAddress, payload.identifier)
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
        val DEFAULT_ADDRESSES = listOf(
            // Dispersy
            // Address("130.161.119.206", 6421),
            // Address("130.161.119.206", 6422),
            // Address("131.180.27.155", 6423),
            // Address("131.180.27.156", 6424),
            // Address("131.180.27.161", 6427),
            // IPv8
            /*
            Address("131.180.27.161", 6521),
            Address("131.180.27.161", 6522),
            Address("131.180.27.162", 6523),
            Address("131.180.27.162", 6524),
            Address("130.161.119.215", 6525),
            Address("130.161.119.215", 6526),
            Address("81.171.27.194", 6527),
            //Address("81.171.27.194", 6528)
             */
            // IPv8 + LibNaCL
            Address("131.180.27.161", 6427)
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
