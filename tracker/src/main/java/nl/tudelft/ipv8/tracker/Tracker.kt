package nl.tudelft.ipv8.tracker

import mu.KotlinLogging
import nl.tudelft.ipv8.*
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.messaging.Packet
import nl.tudelft.ipv8.messaging.payload.IntroductionRequestPayload
import nl.tudelft.ipv8.messaging.udp.UdpEndpoint
import nl.tudelft.ipv8.util.sha1
import nl.tudelft.ipv8.util.toHex
import java.net.InetAddress
import java.util.*

private val logger = KotlinLogging.logger {}

@UseExperimental(ExperimentalUnsignedTypes::class)
class EndpointServer : Community() {
    override val serviceId: String = sha1(cryptoProvider.generateKey().keyToBin()).toHex()

    override fun onPacket(packet: Packet) {
        val sourceAddress = packet.source
        val data = packet.data

        val probablePeer = network.getVerifiedByAddress(sourceAddress)
        if (probablePeer != null) {
            probablePeer.lastResponse = Date()
        }

        val msgId = data[prefix.size].toUByte().toInt()

        if (msgId == MessageId.INTRODUCTION_REQUEST) {
            val packetPrefix = data.copyOfRange(0, prefix.size)
            val (peer, payload) =
                packet.getAuthPayload(IntroductionRequestPayload.Deserializer)
            onGenericIntroductionRequest(peer, payload, packetPrefix)
        } else {
            logger.warn { "Tracker received unknown message $msgId" }
        }
    }

    private fun onGenericIntroductionRequest(
        peer: Peer,
        payload: IntroductionRequestPayload,
        prefix: ByteArray
    ) {
        logger.debug("<- $payload")

        val newPeer = peer.copy(
            lanAddress = payload.sourceLanAddress,
            wanAddress = payload.sourceWanAddress
        )
        addVerifiedPeer(newPeer)

        val introPeers = network.getPeersForService(prefix.toHex())
            .filter { it != peer }
        val introPeer = if (introPeers.isNotEmpty()) introPeers.random() else null

        val packet = createIntroductionResponse(
            newPeer,
            payload.identifier,
            introduction = introPeer,
            prefix = prefix
        )

        send(peer.address, packet)
    }
}

class TrackerService {
    fun startTracker(port: Int) {
        val endpoint = UdpEndpoint(port, InetAddress.getByName("0.0.0.0"))

        val config = IPv8Configuration(
            overlays = listOf(
                OverlayConfiguration(
                    Overlay.Factory(EndpointServer::class.java),
                    // TODO: add simple churn strategy
                    walkers = listOf()
                )
            )
        )
        val key = defaultCryptoProvider.generateKey()
        val ipv8 = IPv8(endpoint, config, key)
        ipv8.start()

        logger.info { "Started tracker" }
    }
}

fun main() {
    // TODO: take port from argument
    val port = 8090
    val service = TrackerService()
    service.startTracker(port)
}
