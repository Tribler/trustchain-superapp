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
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.detoks.TorrentManager
import nl.tudelft.trustchain.detoks.db.OwnedTokenManager
import nl.tudelft.trustchain.detoks.exception.PeerNotFoundException
import nl.tudelft.trustchain.detoks.token.UpvoteToken
import nl.tudelft.trustchain.detoks.token.UpvoteTokenValidator
import nl.tudelft.trustchain.detoks.trustchain.blocks.SeedRewardBlock
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
        messageHandlers[CommunityConstants.SEED_REWARD] = ::onSeedRewardPacket
    }

    private fun onUpvoteTokenPacket(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(UpvoteTokenPayload.Deserializer)
        onUpvoteToken(peer, payload)
    }

    private fun onSeedRewardPacket(packet: Packet) {
        val (_, payload) = packet.getAuthPayload(SeedRewardPayload.Deserializer)
        onSeedReward(payload)
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
        OwnedTokenManager(context).createOwnedUpvoteTokensTable()
        val upvoteTokens = payload.upvoteTokens
        val validTokens: ArrayList<UpvoteToken> = ArrayList()

        Log.i("Detoks", "Received an upvote")

        for (upvoteToken: UpvoteToken in upvoteTokens) {
            if (UpvoteTokenValidator.validateToken(upvoteToken)) {
                validTokens.add(upvoteToken)
            }
        }

        // Check if we received less than 2 valid tokens or minted the video ourselves
        if (validTokens.size < 2 || validTokens[0].publicKeySeeder == myPeer.publicKey.toString()) {
            for (upvoteToken: UpvoteToken in upvoteTokens)
                OwnedTokenManager(context).addReceivedToken(upvoteToken)
            return
        }

        Log.i("Detoks", "Sending seed reward")

        val rewardTokens: ArrayList<UpvoteToken> = ArrayList()

        for (i in 0..CommunityConstants.SEED_REWARD_TOKENS) {
            rewardTokens.add(validTokens.removeFirst())
        }

        for (upvoteToken: UpvoteToken in validTokens)
            OwnedTokenManager(context).addReceivedToken(upvoteToken)
        sendSeedReward(rewardTokens, peer)

        Toast.makeText(context, "Hurray! Received valid token!", Toast.LENGTH_SHORT).show()
    }

    private fun onSeedReward(payload: SeedRewardPayload) {

        OwnedTokenManager(context).createOwnedUpvoteTokensTable()
        val upvoteTokens = payload.upvoteTokens
        val validTokens: ArrayList<UpvoteToken> = ArrayList()

        Log.i("Detoks", "Received seeding reward")
        for (upvoteToken: UpvoteToken in upvoteTokens) {
            if (UpvoteTokenValidator.validateToken(upvoteToken)) {
                OwnedTokenManager(context).addReceivedToken(upvoteToken)
                validTokens.add(upvoteToken)
            }

        }

        val rewardBlockHash = payload.blockHash
        val rewardBlock = database.getBlockWithHash(rewardBlockHash)

        if (rewardBlock == null) {
            Log.e("Detoks", "Failed to find reward block with hash $rewardBlockHash")
            return
        }

        val agreementBlock = createAgreementBlock(rewardBlock, rewardBlock.transaction)
        Log.i("Detoks", "Signed Agreementblock $agreementBlock")

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

    private fun sendSeedReward(upvoteTokens: List<UpvoteToken>, upvotingPeer: Peer) {
        // Prepare the reward block
        val upvoteToken = upvoteTokens[0]
        val rewardBlock = SeedRewardBlock.createRewardBlock(
            upvoteToken.videoID,
            upvoteToken.publicKeySeeder,
            upvotingPeer
        )

        val receiver = getPeers().first { peer -> peer.publicKey.keyToBin().toHex() == (upvoteToken.publicKeySeeder) }

        if (rewardBlock == null) {// || receiver == null) {
            Log.e("Detoks", "Failed to create a reward block")
            return
        }
        val rewardBlockHash = rewardBlock.calculateHash()
        val payload = SeedRewardPayload(rewardBlockHash, upvoteTokens)
        val packet = serializePacket(
            CommunityConstants.SEED_REWARD,
            payload
        )

        send(receiver, packet)
        Log.i("Detoks", "Sent reward block to seeding peer")
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
