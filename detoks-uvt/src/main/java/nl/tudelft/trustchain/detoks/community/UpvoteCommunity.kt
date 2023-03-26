package nl.tudelft.trustchain.detoks.community

import android.content.Context
import android.util.Log
import nl.tudelft.ipv8.Overlay
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.messaging.Packet
import mu.KotlinLogging
import nl.tudelft.ipv8.attestation.trustchain.*
import nl.tudelft.ipv8.attestation.trustchain.store.TrustChainStore
import nl.tudelft.trustchain.detoks.TorrentManager
import nl.tudelft.trustchain.detoks.db.OwnedTokenManager
import nl.tudelft.trustchain.detoks.exception.PeerNotFoundException
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
    var torrentManager: TorrentManager? = null

    init {
        messageHandlers[MessageID.UPVOTE_TOKEN] = ::onUpvoteTokenPacket
        messageHandlers[MessageID.MAGNET_URI_AND_HASH] = ::onMagnetURIPacket
    }

    object MessageID {
        const val UPVOTE_TOKEN = 1
        const val MAGNET_URI_AND_HASH = 2
    }


    private fun onUpvoteTokenPacket(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(UpvoteTokenPayload.Deserializer)
        onUpvoteToken(peer, payload)
    }

    private fun onMagnetURIPacket(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(MagnetURIPayload.Deserializer)
        onMagnetURI(peer, payload)
    }

    private fun onMagnetURI(peer: Peer, payload: MagnetURIPayload) {
        logger.debug { "[MAGNETURIPAYLOAD] -> received magnet payload with uri: ${payload.magnet_uri} and hash: ${payload.proposal_token_hash} from peer with member id: ${peer.mid}" }
//        torrentManager?.addTorrent(payload.magnet_uri)
        torrentManager?.getMagnetLink(payload.magnet_uri)
    }

    private fun onUpvoteToken(peer: Peer, payload: UpvoteTokenPayload) {
        // do something with the payload
        logger.debug { "[UPVOTETOKEN] -> received upvote token with id: ${payload.token_id} from peer with member id: ${peer.mid}" }
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
            MessageID.MAGNET_URI_AND_HASH,
            payload
        )

        val peers = getPeers()
        for (peer in peers) {
            val message = "[MAGNETURIPAYLOAD] You/Peer with member id: ${myPeer.mid} is sending magnet uri to peer with peer id: ${peer.mid}"
            logger.debug { message }
            send(peer, packet)
        }
        return true
//        throw PeerNotFoundException("Could not find a peer")
    }

    /**
     * Sends a UpvoteToken to a random Peer
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
