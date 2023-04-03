package nl.tudelft.trustchain.detoks.community

import android.content.Context
import android.os.Handler
import android.os.Looper
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
import nl.tudelft.trustchain.detoks.db.SentTokenManager
import nl.tudelft.trustchain.detoks.exception.PeerNotFoundException
import nl.tudelft.trustchain.detoks.recommendation.RecommendationType
import nl.tudelft.trustchain.detoks.recommendation.Recommender
import nl.tudelft.trustchain.detoks.token.UpvoteToken
import nl.tudelft.trustchain.detoks.token.UpvoteTokenValidator
import kotlin.math.min
import nl.tudelft.trustchain.detoks.trustchain.blocks.SeedRewardBlock
import nl.tudelft.trustchain.detoks.util.CommunityConstants


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
    var seedVideoIDs = mutableListOf<String>()
    var failedSeeds = mutableListOf<String>()

    init {
        messageHandlers[MessageID.UPVOTE_TOKEN] = ::onUpvoteTokenPacket
        messageHandlers[MessageID.MAGNET_URI_AND_HASH] = ::onMagnetURIPacket
        messageHandlers[MessageID.UPVOTE_VIDEO] = ::onUpvoteVideoPacket
        messageHandlers[MessageID.SEED_REWARD] = ::onSeedRewardPacket
        messageHandlers[MessageID.UPVOTE_TOKEN] = ::onUpvoteTokenPacket
        messageHandlers[MessageID.MAGNET_URI_AND_HASH] = ::onMagnetURIPacket
        this.registerBlockSigner(UpvoteTrustchainConstants.BALANCE_CHECKPOINT, object : BlockSigner {
            override fun onSignatureRequest(block: TrustChainBlock) {
                this@UpvoteCommunity.createAgreementBlock(block, mapOf<Any?, Any?>())
            }
        })
        messageHandlers[MessageID.RECOMMENDATION_REQUEST] = ::onRecommendationRequestPacket
        messageHandlers[MessageID.RECOMMENDATION_RECEIVED] = ::onRecommendationReceivedPacket
    }

    object MessageID {
        const val UPVOTE_TOKEN = 1
        const val RECOMMENDATION_REQUEST = 2
        const val RECOMMENDATION_RECEIVED = 3
        const val MAGNET_URI_AND_HASH = 4
        const val UPVOTE_VIDEO = 5
        const val SEED_REWARD = 6
    }

    object ContentSeeding {
        const val MAX_SEEDED_CONTENT = 5
    }

    fun getContentToSeed() {
        // randomly select peers
        // if a random set is not good, we can change that
        val listOfPostsAndUpvotes = database.getBlocksWithType(UpvoteTrustchainConstants.GIVE_UPVOTE_TOKEN)
        val otherPeersProposalBlocks = listOfPostsAndUpvotes.filter { it.isProposal
            && it.publicKey.toHex() != myPeer.publicKey.keyToBin().toHex()
            && !seedVideoIDs.contains(it.blockId)
            && !failedSeeds.contains(it.blockId)}
        val additionalSeeds = min(otherPeersProposalBlocks.size, ContentSeeding.MAX_SEEDED_CONTENT-seedVideoIDs.size)
        val randomlyChosenProposalBlocks = otherPeersProposalBlocks.shuffled().take(additionalSeeds)
        for (block in randomlyChosenProposalBlocks) {
            val magnetLink = block.transaction["magnetURI"].toString()
            val seeded = torrentManager?.seedTorrentFromMagnetLink(magnetLink)
            if (seeded !=null && seeded){
                seedVideoIDs.add(block.blockId)
                // Greedily send to all other peers who are online
                sendVideoData(magnetLink, block.calculateHash().toHex())
            } else {
                failedSeeds.add(block.blockId)
            }
        }
    }

    private fun onUpvoteTokenPacket(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(UpvoteTokenPayload.Deserializer)
        onUpvoteToken(peer, payload)
    }

    private fun onSeedRewardPacket(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(SeedRewardPayload.Deserializer)
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
        Log.i(
            "Detoks",
            "[MAGNETURIPAYLOAD] -> received magnet payload with uri: ${payload.magnet_uri} and hash: ${payload.proposal_token_hash} from peer with member id: ${peer.mid}"
        )
        logger.debug { "[MAGNETURIPAYLOAD] -> received magnet payload with uri: ${payload.magnet_uri} and hash: ${payload.proposal_token_hash} from peer with member id: ${peer.mid}" }
        torrentManager?.addTorrent(payload.magnet_uri)
    }
    private fun onRecommendationRequestPacket(packet: Packet) {
        val (peer, _) = packet.getAuthPayload(RecommendedVideosPayload.Deserializer)
        sendLastUpvotedVideos(peer)
    }

    private fun onRecommendationReceivedPacket(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(RecommendedVideosPayload.Deserializer)
        logger.debug { "[DETOKS] -> Received recommendations of ${peer.mid}" }
        val recommendations = payload.recommendations
        Recommender.addRecommendations(recommendations, RecommendationType.PEERS)
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
        sendOwnLastVideos(peer)
    }

    fun requestRecommendations() {
        var receivers = getPeers()
        if (receivers.isEmpty())
            return
        val subset = receivers.size

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

        //val test = database.crawl(peer.publicKey.keyToBin(), payload.sequenceNr.toLong(), payload.sequenceNr.toLong() + 1);
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
            MessageID.MAGNET_URI_AND_HASH,
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
     * When a message is sent, a proposal block is created
     * //TODO: only make an agreement block if the user did not like the video yet, if the user already like the video,
     * Sends an UpvoteToken to a random Peer
     */
    fun sendUpvoteToken(upvoteTokens: List<UpvoteToken>): Boolean {
        val payload = UpvoteVideoPayload(upvoteTokens)
        val packet = serializePacket(
            MessageID.UPVOTE_VIDEO,
            payload
        )

        val peer = pickRandomPeer()

        if (peer != null) {
            val message = "[DETOKS] You/Peer with member id: ${myPeer.mid} is sending a upvote token to peer with peer id: ${peer.mid}"
            logger.debug { message }
            send(peer, packet)
            return true
        }
        throw PeerNotFoundException("Could not find a peer")
    }

    private fun sendLastUpvotedVideos(peer: Peer) {
        logger.debug { "[DETOKS] Received request to sent recommended content" }
        val upvotedList = SentTokenManager(context).getFiveLatestUpvotedVideos()
        logger.debug { "[DETOKS] ....and sent $upvotedList back" }
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
        logger.debug { "[DETOKS] Received request to sent recommended content and sent $videoList back" }
        val payload = RecommendedVideosPayload(videoList)

        val packet = serializePacket(
            MessageID.RECOMMENDATION_RECEIVED,
            payload
        )

        send(peer, packet)
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

        if (rewardBlock == null) {
            Log.e("Detoks", "Failed to create a reward block")
            return
        }
        val rewardBlockHash = rewardBlock.calculateHash()
        val payload = SeedRewardPayload(rewardBlockHash, upvoteTokens)
        val packet = serializePacket(
            MessageID.SEED_REWARD,
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
