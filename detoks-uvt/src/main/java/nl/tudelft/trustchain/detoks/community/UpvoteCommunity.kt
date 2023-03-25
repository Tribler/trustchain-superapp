package nl.tudelft.trustchain.detoks.community

import android.content.Context
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.Packet
import mu.KotlinLogging
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.trustchain.detoks.db.OwnedTokenManager
import nl.tudelft.trustchain.detoks.db.SentTokenManager
import nl.tudelft.trustchain.detoks.exception.PeerNotFoundException
import nl.tudelft.trustchain.detoks.recommendation.Recommender
import nl.tudelft.trustchain.detoks.token.UpvoteToken
import nl.tudelft.trustchain.detoks.token.UpvoteTokenValidator

private val logger = KotlinLogging.logger {}

object UpvoteTrustchainConstants {
    const val GIVE_UPVOTE_TOKEN = "give_upvote_token_block"
    const val BALANCE_CHECKPOINT = "balance_checkpoint"
}
class UpvoteCommunity(
    val context: Context,
    settings: TrustChainSettings,
    database: TrustChainStore,
    crawler: TrustChainCrawler = TrustChainCrawler()
) : TrustChainCommunity(settings, database, crawler){
    /**
     * serviceId is a randomly generated hex string with length 40
     */
    override val serviceId = "ee6ce7b5ad81eef11f4fcff335229ba169c03aeb"

    init {
        messageHandlers[MessageID.UPVOTE_TOKEN] = ::onUpvoteTokenPacket
        messageHandlers[MessageID.RECOMMENDATION_REQUEST] = ::onRecommendationRequestPacket
        messageHandlers[MessageID.RECOMMENDATION_RECEIVED] = ::onRecommendationReceivedPacket

    }

    object MessageID {
        const val UPVOTE_TOKEN = 1
        const val RECOMMENDATION_REQUEST = 2
        const val RECOMMENDATION_RECEIVED = 3
    }

    private fun onUpvoteTokenPacket(packet: Packet){
        val (peer, payload) = packet.getAuthPayload(UpvoteTokenPayload.Deserializer)
        onUpvoteToken(peer, payload)
    }

    private fun onRecommendationRequestPacket(packet: Packet) {
        val (peer, _) = packet.getAuthPayload(RecommendedVideosPayload.Deserializer)
        sendLastUpvotedVideos(peer)
    }

    private fun onRecommendationReceivedPacket(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(RecommendedVideosPayload.Deserializer)
        logger.debug { "[DETOKS] -> Received recommendations of ${peer.mid}"}
        val recommendations = payload.recommendations
        Recommender.addRecommendations(recommendations)
    }

    private fun onUpvoteToken(peer: Peer, payload: UpvoteTokenPayload) {
        // do something with the payload
        logger.debug { "[DETOKS] -> received upvote token with id: ${payload.token_id} from peer with member id: ${peer.mid}" }
        val upvoteToken = UpvoteToken(
            payload.token_id.toInt(),
            payload.date,
            payload.public_key_minter,
            payload.video_id
        )

        val isValid = UpvoteTokenValidator.validateToken(upvoteToken)

        if (isValid) {
            // TODO Move table creation to correct place
            OwnedTokenManager(context).createOwnedUpvoteTokensTable()
            OwnedTokenManager(context).addReceivedToken(upvoteToken)
            logger.debug { "[UPVOTETOKEN] Hurray! Received valid token!" }

        } else {
            logger.debug { "[UPVOTETOKEN] Oh no! Received invalid token!" }
        }

        sendOwnLastVideos(peer)

    }

    fun requestRecommendations() {
        var receivers = getPeers()
        if (receivers.isEmpty())
            return
        val subset = receivers.size / 4

        logger.debug { "[DETOKS] Requesting recomendations of ${subset} peers" }

        receivers = receivers.asSequence().shuffled().take(subset).toList()

        val payload = RecommendedVideosPayload(emptyList())

        val packet = serializePacket(
            MessageID.RECOMMENDATION_REQUEST,
            payload
        )

        for (receiver in receivers)
            send(receiver, packet)
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
     * Sends a HeartToken to a random Peer
     * When a message is sent, a proposal block is created
     * //TODO: only make an agreement block if the user did not like the video yet, if the user already like the video,
     * Sends a UpvoteToken to a random Peer
     */
    fun sendUpvoteToken(upvoteToken: UpvoteToken): Boolean {
        val payload = UpvoteTokenPayload(
            upvoteToken.tokenID.toString(),
            upvoteToken.date,
            upvoteToken.publicKeyMinter,
            upvoteToken.videoID)

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

    private fun sendLastUpvotedVideos(peer: Peer) {

        val upvotedList = SentTokenManager(context).getFiveLatestUpvotedVideos()
        logger.debug { "[DETOKS] Received request to sent recommended content" }
        val payload = RecommendedVideosPayload(upvotedList)

        val packet = serializePacket(
            MessageID.RECOMMENDATION_RECEIVED,
            payload
        )

        send(peer, packet)
    }
    private fun sendOwnLastVideos(peer: Peer) {
        logger.debug { "[DETOKS] Sending own content back to peer" }
        val videoList = OwnedTokenManager(context).getLatestThreeUpvotedVideos()

        val payload = RecommendedVideosPayload(videoList)

        val packet = serializePacket(
            MessageID.RECOMMENDATION_RECEIVED,
            payload
        )

        send(peer, packet)
    }
    class Factory(
        private val context: Context,
        private val settings: TrustChainSettings,
        private val database: TrustChainStore,
        private val crawler: TrustChainCrawler = TrustChainCrawler()
    ) : Overlay.Factory<UpvoteCommunity>(UpvoteCommunity::class.java) {
        override fun create(): UpvoteCommunity {
            return UpvoteCommunity(context, settings, database, crawler)
        }
    }

}
