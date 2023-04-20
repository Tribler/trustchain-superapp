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
import nl.tudelft.trustchain.detoks.db.SentTokenManager
import nl.tudelft.trustchain.detoks.recommendation.RecommendationType
import nl.tudelft.trustchain.detoks.recommendation.Recommender
import nl.tudelft.trustchain.detoks.services.SeedRewardService
import nl.tudelft.trustchain.detoks.services.UpvoteService
import nl.tudelft.trustchain.detoks.token.UpvoteToken
import kotlin.math.min
import nl.tudelft.trustchain.detoks.trustchain.blocks.SeedRewardBlock



private val logger = KotlinLogging.logger {}

object UpvoteTrustChainConstants {
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
    var seedVideoIDs = mutableListOf<Pair<String, String>>()
    var failedSeeds = mutableListOf<Pair<String, String>>()

    private var upvoteService: UpvoteService
    private var seedRewardService: SeedRewardService

    init {
        messageHandlers[MessageID.MAGNET_URI_AND_HASH] = ::onMagnetURIPacket
        messageHandlers[MessageID.UPVOTE_VIDEO] = ::onUpvoteVideoPacket
        messageHandlers[MessageID.SEED_REWARD] = ::onSeedRewardPacket
        this.registerBlockSigner(UpvoteTrustChainConstants.BALANCE_CHECKPOINT, object : BlockSigner {
            override fun onSignatureRequest(block: TrustChainBlock) {
                this@UpvoteCommunity.createAgreementBlock(block, mapOf<Any?, Any?>())
            }
        })
        messageHandlers[MessageID.RECOMMENDATION_REQUEST] = ::onRecommendationRequestPacket
        messageHandlers[MessageID.RECOMMENDATION_RECEIVED] = ::onRecommendationReceivedPacket
        messageHandlers.entries.forEach { Log.i("Detoks", "key is ${it.key} and function is ${it.value.toString()}") }

        upvoteService = UpvoteService(OwnedTokenManager(context), SentTokenManager(context))
        seedRewardService = SeedRewardService(OwnedTokenManager(context))

    }

    object MessageID {
        const val RECOMMENDATION_REQUEST = 9
        const val RECOMMENDATION_RECEIVED = 10
        const val MAGNET_URI_AND_HASH = 11
        const val UPVOTE_VIDEO = 12
        const val SEED_REWARD = 13
    }

    object ContentSeeding {
        const val MAX_SEEDED_CONTENT = 5
    }

    fun getContentToSeed() {

        val listOfPostsAndUpvotes = database.getBlocksWithType(UpvoteTrustChainConstants.GIVE_UPVOTE_TOKEN)
        val otherPeersProposalBlocks = listOfPostsAndUpvotes.filter { it.isProposal
            && it.publicKey.toHex() != myPeer.publicKey.keyToBin().toHex()
            && seedVideoIDs.firstOrNull { pair -> pair.first == it.calculateHash().toHex()} == null
            && failedSeeds.firstOrNull { pair -> pair.first == it.calculateHash().toHex()} == null
            && it.transaction.containsKey("magnetURI")
        }
        val additionalSeeds = min(otherPeersProposalBlocks.size, ContentSeeding.MAX_SEEDED_CONTENT-seedVideoIDs.size)
        val randomlyChosenProposalBlocks = otherPeersProposalBlocks.shuffled().take(additionalSeeds)
        Log.i("Detoks", "getContentToSeed: otherPeersProposalBlocks size: ${otherPeersProposalBlocks.size}, additional seeds: ${additionalSeeds}, randomly chosen size : ${randomlyChosenProposalBlocks.size
        }")
        for (block in randomlyChosenProposalBlocks) {
            Log.i("Detoks", block.transaction.toString())
        }
        for (block in randomlyChosenProposalBlocks) {
            val magnetLink = block.transaction["magnetURI"].toString()
            Log.i("Detoks", "The magnet link is: $magnetLink")
            val seeded = torrentManager?.seedTorrentFromMagnetLink(magnetLink)
            if (seeded !=null && seeded){
                seedVideoIDs.add(Pair(block.calculateHash().toHex(), magnetLink))
                // Greedily send to all other peers who are online
            } else {
                failedSeeds.add(Pair(block.calculateHash().toHex(), magnetLink))
            }
        }
        for (pair in seedVideoIDs) {
            // greedily sending seeded content to all other peers
            sendVideoData(pair.second, pair.first)
        }
        Log.i("Detoks", "${seedVideoIDs.size}")
        seedVideoIDs.forEach { Log.i("Detoks", "Your peer is seeding $it") }
    }

    private fun onSeedRewardPacket(packet: Packet) {
        val (_, payload) = packet.getAuthPayload(SeedRewardPayload.Deserializer)
        onSeedReward(payload)
    }

    private fun onUpvoteVideoPacket(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(UpvoteVideoPayload.Deserializer)
        onUpvoteVideo(peer, payload)
    }

    private fun onRecommendationRequestPacket(packet: Packet) {
        val (peer, _) = packet.getAuthPayload(RecommendedVideosPayload.Deserializer)
        sendLastUpvotedVideos(peer)
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
        Log.i(
            "Detoks",
            "function: onMagnetURI: attempting to get block with the following hash: ${payload.proposal_token_hash}"
        )
        val block = this.database.getBlockWithHash(payload.proposal_token_hash.hexToBytes())
        if (block != null) {
            torrentManager?.addTorrent(payload.magnet_uri, payload.proposal_token_hash, block.timestamp.toString(), block.blockId)
            Log.i("Detoks", "[Detoks] -> Success! received magnet payload with uri: ${payload.magnet_uri} and hash: ${payload.proposal_token_hash} from peer with member id: ${peer.mid}")
        } else {
            Log.i("Detoks", "failed to add torrent to video feed because block is null \n attempted to get block with hash: ${payload.proposal_token_hash}")
        }
    }

    private fun onRecommendationReceivedPacket(packet: Packet) {
        val (peer, payload) = packet.getAuthPayload(RecommendedVideosPayload.Deserializer)
        logger.debug { "[DETOKS] -> Received recommendations of ${peer.mid}" }
        val recommendations = payload.recommendations
        Recommender.addRecommendations(recommendations, RecommendationType.PEERS)
    }

    fun requestRecommendations() {
        var receivers = getPeers()
        if (receivers.isEmpty())
            return
        val subset = receivers.size

        Log.i("Detoks", "Requesting recommendations of $subset peers")

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

        val validTokens: ArrayList<UpvoteToken> = upvoteService.getValidUpvoteTokens(payload.upvoteTokens)

        // Check if we should reward the seeder
        if (validTokens[0].publicKeySeeder != myPeer.publicKey.toString()) {
            val rewardTokens: ArrayList<UpvoteToken> = upvoteService.getRewardTokens(validTokens)
            sendSeedReward(rewardTokens, peer)
            Log.i("Detoks", "Sending seed reward")
        }
        Log.i("Detoks", "Received ${validTokens.size} tokens!")
        upvoteService.persistTokens(validTokens)

        // Send recommendations back to the peer that upvoted the video
        sendOwnLastVideos(peer)
        Toast.makeText(context, "Hurray! Received valid tokens!", Toast.LENGTH_SHORT).show()
    }

    /**
     * Handles incoming SeedReward packets. It checks if the given reward is valid and signs
     * the Agreement block if the reward is valid.
     */
    private fun onSeedReward(payload: SeedRewardPayload) {

        val validReward = seedRewardService.handleReward(payload.upvoteTokens)

        if (!validReward) {
            Log.i("Detoks", "Received an invalid seed reward")
            return
        }

        val rewardBlockHash = payload.blockHash
        val rewardBlock = database.getBlockWithHash(rewardBlockHash)

        if (rewardBlock == null) {
            val hashToString = rewardBlockHash.toHex()
            Log.e("Detoks", "Failed to find reward block with hash $hashToString")
            return
        }

        val agreementBlock = createAgreementBlock(rewardBlock, rewardBlock.transaction)
        Log.i("Detoks", "Signed Seed Reward Agreement block $agreementBlock")

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
            Log.i("Detoks", "No peers are online at this moment, you/peer with mid :${myPeer.mid} cannot sent (hash,magnetUri) = (${proposalTokenHash},${magnetURI}) to anyone")
        }
        for (peer in peers) {
            Log.i("Detoks", "This peer with peer mid is online: ${peer.mid}")
            val message = "[MAGNETURIPAYLOAD] You/Peer with member id: ${myPeer.mid} is sending magnet uri to peer with peer id: ${peer.mid} \n" +  "the magnet URI is $magnetURI"
            Log.i("Detoks", message)
            logger.debug { message }
            send(peer, packet)
        }
        send(myPeer, packet)
        return true
    }

    /**
     * Send tokens to the peer that posted the video that the user upvoted by double tapping
     */
    fun sendUpvoteToken(upvoteTokens: List<UpvoteToken>, receiverPublicKey: ByteArray): Boolean {
        val payload = UpvoteVideoPayload(upvoteTokens)
        val packet = serializePacket(
            MessageID.UPVOTE_VIDEO,
            payload
        )

        val peers = getPeers()
        val peer = peers.firstOrNull { it.publicKey.keyToBin().contentEquals(receiverPublicKey) }

        if (peer != null) {
            Log.i("Detoks", "You/Peer with member id: ${myPeer.mid} is sending a upvote token to peer with peer id: ${peer.mid}")
            send(peer, packet)
            return true
        }
        Log.i("Detoks", "Did not find a peer with pubkey ${receiverPublicKey.toHex()} to send upvote token to")
        return false
    }

    private fun sendLastUpvotedVideos(peer: Peer) {
        val upvotedList = upvoteService.getFiveLatestUpvotedVideos()
        Log.i("Detoks", "Received request to sent recommended content and sent $upvotedList back")
        val payload = RecommendedVideosPayload(upvotedList)
        val packet = serializePacket(
            MessageID.RECOMMENDATION_RECEIVED,
            payload
        )

        send(peer, packet)
    }
    private fun sendOwnLastVideos(peer: Peer) {

        val videoList = upvoteService.getLatestThreeUpvotedVideos()
        Log.i("Detoks", "Sending upvoted content $videoList back")
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
