package nl.tudelft.trustchain.detoks.community

import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.Packet
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class UpvoteCommunity() : Community(){
    /**
     * serviceId is a randomly generated hex string with length 40
     */
    override val serviceId = "ee6ce7b5ad81eef11f4fcff335229ba169c03aeb"

    init {
        messageHandlers[MessageID.HEART_TOKEN] = ::onHeartTokenPacket
    }

    object MessageID {
        const val HEART_TOKEN = 1
    }

    private fun onHeartTokenPacket(packet: Packet){
        val (peer, payload) = packet.getAuthPayload(HeartTokenPayload.Deserializer)
        onHeartToken(peer, payload)
    }

    private fun onHeartToken(peer: Peer, payload: HeartTokenPayload) {
        // do something with the payload
        logger.debug { "-> received heart token with id: ${payload.id}  and token: ${payload.token} from peer with member id: ${peer.mid}" }
    }

    /**
     * Use this function in the DetoksFragment class to send a Heart Token
     */
    fun sendHeartToken(id: String, token: String, peer: Peer) {
        val payload = HeartTokenPayload(id, token)

        val packet = serializePacket(
            MessageID.HEART_TOKEN,
            payload
        )
        logger.debug { "You/Peer with member id: ${myPeer.mid} is sending a heart token to peer with peer id: ${peer.mid}" }
        send(peer, packet)
    }

    class Factory(
        // add parameters needed by the constructor of UpvoteCommunity if needed
    ) : Overlay.Factory<UpvoteCommunity>(UpvoteCommunity::class.java) {
        override fun create(): UpvoteCommunity {
            return UpvoteCommunity()
        }
    }
}
