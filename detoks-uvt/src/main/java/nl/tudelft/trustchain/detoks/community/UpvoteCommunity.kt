package nl.tudelft.trustchain.detoks.community

import android.content.Context
import android.util.Log
import android.widget.Toast
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.Packet
import mu.KotlinLogging
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.trustchain.detoks.TorrentManager
import nl.tudelft.trustchain.detoks.db.OwnedTokenManager
import nl.tudelft.trustchain.detoks.db.SentTokenManager
import nl.tudelft.trustchain.detoks.exception.PeerNotFoundException
import nl.tudelft.trustchain.detoks.token.UpvoteToken
import nl.tudelft.trustchain.detoks.token.UpvoteTokenValidator
import nl.tudelft.trustchain.detoks.util.CommunityConstants

private val logger = KotlinLogging.logger {}

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
    var torrentManager: TorrentManager? = null

    init {
        messageHandlers[CommunityConstants.UPVOTE_TOKEN] = ::onUpvoteTokenPacket
        messageHandlers[CommunityConstants.MAGNET_URI_AND_HASH] = ::onMagnetURIPacket
        messageHandlers[CommunityConstants.UPVOTE_VIDEO] = ::onUpvoteVideoPacket
    }

    private fun onUpvoteTokenPacket(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(UpvoteTokenPayload.Deserializer)
        onUpvoteToken(peer, payload)
    }

    private fun onUpvoteVideoPacket(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(UpvoteVideoPayload.Deserializer)
        onUpvoteVideo(peer, payload)
    }


    private fun onMagnetURIPacket(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(MagnetURIPayload.Deserializer)
        onMagnetURI(peer, payload)
    }

    private fun onMagnetURI(peer: Peer, payload: MagnetURIPayload) {
        Log.i("Detoks", "[MAGNETURIPAYLOAD] -> received magnet payload with uri: ${payload.magnet_uri} and hash: ${payload.proposal_token_hash} from peer with member id: ${peer.mid}")
        logger.debug { "[MAGNETURIPAYLOAD] -> received magnet payload with uri: ${payload.magnet_uri} and hash: ${payload.proposal_token_hash} from peer with member id: ${peer.mid}" }
        torrentManager?.addTorrent(payload.magnet_uri)
    }

    private fun onUpvoteToken(peer: Peer, payload: UpvoteTokenPayload) {
        OwnedTokenManager(context).createOwnedUpvoteTokensTable()
        // do something with the payload
        logger.debug { "[DETOKS] -> received upvote token with id: ${payload.token_id} from peer with member id: ${peer.mid}" }
        val upvoteToken = UpvoteToken(
            payload.token_id.toInt(),
            payload.date,
            payload.public_key_minter,
            payload.video_id,
            ""
        )

        val isValid = UpvoteTokenValidator.validateToken(upvoteToken)

        if (isValid) {
            OwnedTokenManager(context).addReceivedToken(upvoteToken)
            logger.debug { "[UPVOTETOKEN] Hurray! Received valid token!" }

        } else {
            logger.debug { "[UPVOTETOKEN] Oh no! Received invalid token!" }
        }

    }

    private fun onUpvoteVideo(peer: Peer, payload: UpvoteVideoPayload) {
        // do something with the payload

        OwnedTokenManager(context).createOwnedUpvoteTokensTable()
        val upvoteTokens = payload.upvoteTokens
        val receivedTokenIDs: ArrayList<Int> = ArrayList()

        for (upvoteToken: UpvoteToken in upvoteTokens) {
            logger.debug { "[UPVOTETOKEN] -> received upvote token with id: ${upvoteToken.tokenID} from peer with member id: ${peer.mid}" }

            if (UpvoteTokenValidator.validateToken(upvoteToken)) {
                OwnedTokenManager(context).addReceivedToken(upvoteToken)
                receivedTokenIDs.add(upvoteToken.tokenID)
            }

        }

        if (receivedTokenIDs.isEmpty())
            return

        Toast.makeText(context, "Hurray! Received valid token!", Toast.LENGTH_SHORT).show()


    }
    /**
     * Selects a random Peer from the list of known Peers
     * @returns A random Peer or null if there are no known Peers
     */
    fun pickRandomPeer(): Peer? {
        val peers = getPeers()
        for (peer in peers) {
            Log.i("Detoks", "This peer with peer mid is online: ${peer.mid}")
        }
        if (peers.isEmpty()) return null
        return peers.random()
    }

    fun sendVideoData(magnetURI: String, proposalTokenHash: String): Boolean {
        val payload = MagnetURIPayload(
            magnetURI,
            proposalTokenHash
        )

        val packet = serializePacket(
            CommunityConstants.MAGNET_URI_AND_HASH,
            payload
        )

        val peers = getPeers()
        if (peers.isEmpty()) {
            Log.i("Detoks", "No peers are online at this momemt, you/peer with mid :${myPeer.mid} cannot sent (hash,magnetUri) = (${proposalTokenHash},${magnetURI}) to anyone")
            throw PeerNotFoundException("Could not find a peer")
        }
        for (peer in peers) {
            Log.i("Detoks", "This peer with peer mid is online: ${peer.mid}")
            val message = "[MAGNETURIPAYLOAD] You/Peer with member id: ${myPeer.mid} is sending magnet uri to peer with peer id: ${peer.mid}"
            Log.i("Detoks", message)
            logger.debug { message }
            send(peer, packet)
        }
        return true

//        val peer = pickRandomPeer()
//
//        if (peer != null) {
//            val message = "[MAGNETURIPAYLOAD] You/Peer with member id: ${myPeer.mid} is sending magnet uri to peer with peer id: ${peer.mid}"
//            Log.i("Detoks", message)
//            logger.debug { message }
//            send(peer, packet)
//            return true
//        }
//        throw PeerNotFoundException("Could not find a peer")
    }

    /**
     * Sends a UpvoteToken to a random Peer
     * When a message is sent, a proposal block is created
     * //TODO: only make an agreement block if the user did not like the video yet, if the user already like the video,
     * Sends a UpvoteToken to a random Peer
     */
    fun sendUpvoteToken(upvoteTokens: List<UpvoteToken>): Boolean {
        val payload = UpvoteVideoPayload(upvoteTokens)
        val packet = serializePacket(
            CommunityConstants.UPVOTE_VIDEO,
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
