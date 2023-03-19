package nl.tudelft.trustchain.detoks.community

import nl.tudelft.ipv8.Community
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.Packet
import mu.KotlinLogging
import nl.tudelft.trustchain.detoks.exception.PeerNotFoundException
import nl.tudelft.trustchain.detoks.token.UpvoteToken
import nl.tudelft.trustchain.detoks.token.UpvoteTokenValidator

private val logger = KotlinLogging.logger {}

class UpvoteCommunity() : Community(){
    /**
     * serviceId is a randomly generated hex string with length 40
     */
    override val serviceId = "ee6ce7b5ad81eef11f4fcff335229ba169c03aeb"

    init {
        messageHandlers[MessageID.UPVOTE_TOKEN] = ::onUpvoteTokenPacket
    }

    object MessageID {
        const val UPVOTE_TOKEN = 1
    }

    private fun onUpvoteTokenPacket(packet: Packet){
        val (peer, payload) = packet.getAuthPayload(UpvoteTokenPayload.Deserializer)
        onUpvoteToken(peer, payload)
    }

    private fun onUpvoteToken(peer: Peer, payload: UpvoteTokenPayload) {
        // do something with the payload
        logger.debug { "[UPVOTETOKEN] -> received upvote token with id: ${payload.token_id} from peer with member id: ${peer.mid}" }
        val upvoteToken = UpvoteToken(
            payload.token_id.toInt(),
            payload.date,
            payload.public_key_minter,
            payload.video_id.toInt()
        )

        val isValid = UpvoteTokenValidator.validateToken(upvoteToken)

        if (isValid) {
            logger.debug { "[UPVOTETOKEN] Hurray! Received valid token!" }
        } else {
            logger.debug { "[UPVOTETOKEN] Oh no! Received invalid token!" }
        }

    }

    /**
     * Selects a random Peer from the list of known Peers
     * @returns A random Peer or null if there are no known Peers
     */
    private fun pickRandomPeer(): Peer? {
        val peers = getPeers()
        if (peers.isEmpty()) return null
        return peers.random()
    }

    /**
     * Sends a UpvoteToken to a random Peer
     */
    fun sendUpvoteToken(upvoteToken: UpvoteToken): Boolean {
        val payload = UpvoteTokenPayload(
            upvoteToken.tokenID.toString(),
            upvoteToken.date,
            upvoteToken.publicKeyMinter,
            upvoteToken.videoID.toString())

        val packet = serializePacket(
            MessageID.UPVOTE_TOKEN,
            payload
        )

        val peer = pickRandomPeer()

        if (peer != null) {
            val message = "[UPVOTETOKEN] You/Peer with member id: ${myPeer.mid} is sending a upvote token to peer with peer id: ${peer.mid}"
            logger.debug { message }
            send(peer, packet)
            return true
        }
        throw PeerNotFoundException("Could not find a peer")
    }

    class Factory(
        // add parameters needed by the constructor of UpvoteCommunity if needed
    ) : Overlay.Factory<UpvoteCommunity>(UpvoteCommunity::class.java) {
        override fun create(): UpvoteCommunity {
            return UpvoteCommunity()
        }
    }
}
