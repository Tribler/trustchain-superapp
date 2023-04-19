package nl.tudelft.trustchain.detoks.token

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Toast
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.messaging.serializeVarLen
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.detoks.community.UpvoteCommunity
import nl.tudelft.trustchain.detoks.community.UpvoteTrustChainConstants
import nl.tudelft.trustchain.detoks.db.SentTokenManager
import nl.tudelft.trustchain.detoks.exception.InvalidMintException
import nl.tudelft.trustchain.detoks.exception.PeerNotFoundException
import nl.tudelft.trustchain.detoks.helpers.DoubleClickListener
import nl.tudelft.trustchain.detoks.util.CommunityConstants
import kotlin.collections.ArrayList

class UpvoteToken constructor(
    val tokenID: Int,
    val date: String,
    val publicKeyMinter: String,
    var videoID: String,
    val publicKeySeeder: String
) {

    constructor() : this(-1, "", "", "", "")

    fun toByteArray(): ByteArray {
        return serializeVarLen(tokenID.toString().toByteArray()) +
            serializeVarLen(date.toByteArray()) +
            serializeVarLen(publicKeyMinter.toByteArray()) +
            serializeVarLen(videoID.toByteArray()) +
            serializeVarLen(publicKeySeeder.toByteArray())
    }

    override fun equals(other: Any?): Boolean {
        if (other is UpvoteToken) {
            return tokenID == other.tokenID
                && date == other.date
                && publicKeyMinter == other.publicKeyMinter
                && videoID == other.videoID
                && publicKeySeeder == other.publicKeySeeder
        }
        return false
    }

    /**
     * Upvotes the current video, if the video is not already upvoted. The result will be displayed
     * in a ToastMessage to the User.
     * @param context the context of the application.
     */
    fun upvoteVideo(context: Context) {

        val upvoteCommunity = IPv8Android.getInstance().getOverlay<UpvoteCommunity>()

        if (upvoteCommunity == null) {
            createToastMessage("Could not load UpvoteCommunity", context)
            return
        }

        val currentVideoHash = upvoteCommunity.torrentManager?.getHashOfCurrentVideo()!!.hexToBytes()
        val proposalBlock = upvoteCommunity.database.getBlockWithHash(currentVideoHash)

        if (proposalBlock == null) {
            createToastMessage("Attempted to find a proposal Block with hash: \n ${currentVideoHash.toHex()}, \nThis video does not have a proposal block attached to it \n and is thus not posted by anyone", context)
            return
        }

        val myPubKey = upvoteCommunity.myPeer.publicKey.keyToBin().toHex()
        if (isAlreadySigned(upvoteCommunity, proposalBlock, myPubKey)) {
            Log.i("DeToks", "You already liked this video, cannot like again")
            createToastMessage("Cannot like a video more than once!", context)
            return
        }

        upvoteCommunity.createAgreementBlock(proposalBlock, proposalBlock.transaction)
        Log.i("DeToks", "Agreement block created!")

        val seedingMagnetUri = upvoteCommunity.torrentManager?.seedLikedVideo()

        if (seedingMagnetUri != null)
            createToastMessage("Upvoted, now seeding this video: $seedingMagnetUri", context)
        else
            createToastMessage("Upvoted, but failed to seed this video", context)

        var publicKeySeeder = getPublicKeyOfSeeder(upvoteCommunity)

        if (publicKeySeeder == null) {
            // Could not find the seeder, all rewards go to the peer who posted the video
            Log.i("Detoks", "Could not find the seeder of this video you liked, all minted tokens will go the peer who posted this video you upvoted")
            publicKeySeeder = proposalBlock.publicKey.toHex()
        }

        createToastMessage("reward seeder with this pub key: \n $publicKeySeeder", context)

        val upvoteTokenList = mintTokens(context, proposalBlock.blockId, myPubKey, publicKeySeeder)

        if (upvoteTokenList.isEmpty()) {
            createToastMessage("Could not mint tokens to upvote the video", context)
            return
        }

        val sendSuccess = upvoteCommunity.sendUpvoteToken(upvoteTokenList, proposalBlock.publicKey)
        if (sendSuccess) {
            createToastMessage("Successfully sent the tokens to the creator of ${proposalBlock.blockId}", context)
        } else {
            createToastMessage("Failed to sent the tokens \n to the creator of ${proposalBlock.blockId}", context)
        }

    }

    /**
     * Gets the public key of the peer that seeded the most bytes
     * @param upvoteCommunity the UpvoteCommunity
     * @return The public key of the peer that seeded the most bytes
     */
    private fun getPublicKeyOfSeeder(upvoteCommunity: UpvoteCommunity): String? {
        val seedersIPAddress =  upvoteCommunity.torrentManager?.mvpSeeder()?.sortedByDescending { it.third }
        Log.i("Detoks", "now logging peers who seeded this current vid")
        seedersIPAddress?.forEach { it -> Log.i("Detoks", "client: ${it.first}, ip: ${it.second}, download bytes: ${it.third}") }

        val allPeers = upvoteCommunity.getPeers()
        val chosenSeeder = seedersIPAddress?.firstOrNull {
            allPeers.map { peer -> peer.lanAddress.ip }.contains(it.second.substringBefore(":")) ||
                allPeers.map { peer -> peer.wanAddress.ip }.contains(it.second.substringBefore(":"))
        }
        return allPeers.firstOrNull { peer ->
            peer.lanAddress.ip == chosenSeeder?.second?.substringBefore(":") || peer.wanAddress.ip == chosenSeeder?.second?.substringBefore(":")  }
            ?.publicKey?.keyToBin()
            ?.toHex()

    }

    /**
     * Checks if the proposal block is already singed by the peer with the given public key
     * @param upvoteCommunity the [UpvoteCommunity].
     * @param proposalBlock the proposal block the check for.
     * @param pubKey the public key of the peer to check for.
     * @return true, if and only if, the block is already signed by the peer.
     */
    private fun isAlreadySigned(upvoteCommunity: UpvoteCommunity, proposalBlock: TrustChainBlock, pubKey: String): Boolean {
        val linkedBlocks = upvoteCommunity.database.getAllLinked(proposalBlock)
        val alreadyLiked = linkedBlocks.find { it.type == UpvoteTrustChainConstants.GIVE_UPVOTE_TOKEN
            && it.publicKey.toHex().contentEquals(pubKey) && it.isAgreement}
        return alreadyLiked != null
    }

    /**
     * Mints the [UpvoteToken]s required to upvote a video.
     * @param context the context of the application.
     * @param videoID the ID of the video to upvote.
     * @param myPubKey the public key of the minter of the [UpvoteToken]s.
     * @param seederPublicKey the public key of the seeder that should be rewarded.
     * @return the list of minted [UpvoteToken]s or an empty list if an exception occurred.
     */
    private fun mintTokens(context: Context, videoID: String, myPubKey: String, seederPublicKey: String): List<UpvoteToken> {
        val upvoteTokenFactory = UpvoteTokenFactory(SentTokenManager(context))

        try {
            return upvoteTokenFactory.tryMintMultipleTokens(videoID, myPubKey, seederPublicKey, CommunityConstants.TOKENS_SENT_PER_UPVOTE)
        } catch (invalidMintException: InvalidMintException) {
            createToastMessage(invalidMintException.message, context)
        } catch (peerNotFoundException: PeerNotFoundException) {
            createToastMessage(peerNotFoundException.message, context)
        }
        return ArrayList()
    }

    /**
     * Simple helper function to create [Toast]s.
     * @param message the message to display.
     * @param context the context of the application.
     */
    private fun createToastMessage(message: String?, context: Context) {
        Toast.makeText(
            context,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    /**
     * Sets a listener to like a video by double tapping the screen
     */
    fun setLikeListener(itemView: View) {
        val adapter = this

        itemView.setOnClickListener(
            object : DoubleClickListener() {
                override fun onDoubleClick(view: View?) {
                    adapter.upvoteVideo(itemView.context)
                }
            }
        )
    }
}
