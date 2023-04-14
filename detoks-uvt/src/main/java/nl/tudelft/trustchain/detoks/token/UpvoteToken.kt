package nl.tudelft.trustchain.detoks.token

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.messaging.serializeVarLen
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.detoks.community.UpvoteCommunity
import nl.tudelft.trustchain.detoks.community.UpvoteTrustchainConstants
import nl.tudelft.trustchain.detoks.db.SentTokenManager
import nl.tudelft.trustchain.detoks.helpers.DateFormatter
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
    companion object {
        fun tryMintToken(context: Context, videoID: String, publicKey: String, publicKeySeeder: String): UpvoteToken {
            SentTokenManager(context).createSentUpvoteTokensTable()
            val lastUpvoteToken = SentTokenManager(context).getLastToken()
            // Check if we have sent a token already today
            val today = DateFormatter.startOfToday()
            var newToken: UpvoteToken
            // Check if a new sequence should be started
            if (lastUpvoteToken == null
                || DateFormatter.stringToDate(lastUpvoteToken.date).before(today)) {
                newToken = UpvoteToken(0, DateFormatter.todayAsString(), publicKey, videoID, publicKeySeeder)
            } else if (lastUpvoteToken.tokenID > -1 && lastUpvoteToken.tokenID < CommunityConstants.DAILY_MINT_LIMIT) {
                val nextId = lastUpvoteToken.tokenID + 1
                newToken = UpvoteToken(nextId, DateFormatter.todayAsString(), publicKey, videoID, publicKeySeeder)
            } else {
                throw InvalidMintException("Mint limit exceeded")
            }
            newToken.videoID += DateFormatter.startOfToday()
            newToken.videoID += newToken.tokenID
            return newToken
        }
    }

    /**
     * Sends a UpvoteToken to a random user and displays the result in a toast message
     * TODO: change this function so that:
     * - it gets the hash or proposal block of the video that is currently displayed on the
     * scree
     * - checks if this peer/user already liked or already created a proposal block for the
     * proposal block of this video -> if already liked once => show message to user / cannot like again
     * - if not then create an agreement block for this video
     */
    fun sendUpvoteToken(itemView: View, videoID: String) {

        val upvoteCommunity = IPv8Android.getInstance().getOverlay<UpvoteCommunity>()
        val seedersIPAddress =  upvoteCommunity?.torrentManager?.mvpSeeder()?.sortedByDescending { it.third }
        Log.i("Detoks", "now logging peers who seeded this current vid")
        seedersIPAddress?.forEach { it -> Log.i("Detoks", "client: ${it.first}, ip: ${it.second}, download bytes: ${it.third}") }
        val allPeers = upvoteCommunity?.getPeers()!!
        val chosenSeeder = seedersIPAddress?.firstOrNull {
            allPeers.map { peer -> peer.lanAddress.ip }.contains(it.second.substringBefore(":")) ||
                allPeers.map { peer -> peer.wanAddress.ip }.contains(it.second.substringBefore(":"))
        }
        var chosenSeederPublicKey = allPeers.firstOrNull { peer ->
            peer.lanAddress.ip == chosenSeeder?.second?.substringBefore(":") || peer.wanAddress.ip == chosenSeeder?.second?.substringBefore(":")  }
            ?.publicKey?.keyToBin()
            ?.toHex()

        val myPubKey = upvoteCommunity.myPeer.publicKey.keyToBin().toHex()
        //val upvoteToken = UpvoteToken(1, "1679006615", "12345678910", 1)
        //val toastMessage = upvoteCommunity?.sendUpvoteToken(upvoteToken.tokenID.toString(), localToGMT(upvoteToken.date.toLong()).toString(), upvoteToken.publicKeyMinter, upvoteToken.videoID.toString())

        // check if already liked by self
        // getBlockWithHash Method below might fail to get the proposal block if it is not in this peer's truststore
//        upvoteCommunity?.torrentManager?.getHashOfCurrentVideo()!!.hexToBytes()
        val currentVideoHash = upvoteCommunity.torrentManager?.getHashOfCurrentVideo()!!.hexToBytes()
        val proposalBlock = upvoteCommunity.database.getBlockWithHash(currentVideoHash)
        if (proposalBlock != null) {
            val linkedBlocks = upvoteCommunity.database.getAllLinked(proposalBlock)
            val alreadyLiked = linkedBlocks.find { it.type == UpvoteTrustchainConstants.GIVE_UPVOTE_TOKEN
                && it.publicKey.toHex().contentEquals(myPubKey) && it.isAgreement}
            if (alreadyLiked != null) {
                Log.i("DeToks", "You already liked this video, cannot like again")
                Toast.makeText(
                    itemView.context,
                    "Cannot like a video more than once!",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            upvoteCommunity.createAgreementBlock(proposalBlock, proposalBlock.transaction)
            val seedingMagnetUri = upvoteCommunity.torrentManager?.seedLikedVideo()
            upvoteCommunity.torrentManager?.mvpSeeder()
            var toastMsg: String? = if (seedingMagnetUri != null) {
                "Agreement block created, upvoted successfully and now seeding liked video with magnetURI: $seedingMagnetUri"
            } else {
                "Agreement block created, upvoted successfully, but failed to seed this video you liked"
            }
            Log.i("DeToks", "Agreement block created!")
            Toast.makeText(
                itemView.context,
                toastMsg  ,
                Toast.LENGTH_SHORT
            ).show()

            var toastMessage: String?

            if (chosenSeederPublicKey == null) {
                // Could not find the seeder, all rewards go to the peer who posted the video
                Log.i("Detoks", "Could not find the seeder of this video you liked, all minted tokens will go the peer who posted this video you upvoted")
                chosenSeederPublicKey = proposalBlock.publicKey.toHex()
            }

            try {
                val upvoteTokenList: ArrayList<UpvoteToken> = ArrayList()
                val tokenIDList: ArrayList<Int> = ArrayList()

                var dbSuccess: Boolean

                // Mint the required amount of tokens to upvote the video
                while (upvoteTokenList.size < CommunityConstants.TOKENS_SENT_PER_UPVOTE) {
                    val nextToken = tryMintToken(itemView.context, videoID, myPubKey, chosenSeederPublicKey)
                    dbSuccess = SentTokenManager(itemView.context).addSentToken(nextToken)

                    if (!dbSuccess)
                        throw InvalidMintException("Could not add the minted token to the database")
                    upvoteTokenList.add(nextToken)
                    tokenIDList.add(nextToken.tokenID)
                }
                //FIXME fix upvoteCommunity.sendUpvoteToken(upvoteTokenList) => it currently sends the token to a random peer which is wrong!!!
                val sendSuccess = upvoteCommunity.sendUpvoteToken(upvoteTokenList)
                toastMessage = if (sendSuccess == true) {
                    "Successfully sent the token ${tokenIDList.joinToString(", ")} to the creator of $videoID"
                } else {
                    "Failed to sent the token ${tokenIDList.joinToString(", ")} to the creator of $videoID"
                }

            } catch (invalidMintException: InvalidMintException) {
                toastMessage = invalidMintException.message
            } catch (peerNotFoundException: PeerNotFoundException) {
                // TODO Add DB Rollback and potential other network failures
                toastMessage = peerNotFoundException.message
            }
            // Toast the result
            Toast.makeText(
                itemView.context,
                toastMessage,
                Toast.LENGTH_SHORT
            ).show()

        } else {
            Toast.makeText(
                itemView.context,
                "Attempted to find a proposal Block with hash: $currentVideoHash, This video does not have a proposal block attached to it and is thus not posted by anyone",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
    }

    /**
     * Sets a listener to like a video by double tapping the screen
     */
    fun setLikeListener(itemView: View, videoID: String) {
        val adapter = this

        itemView.setOnClickListener(
            object : DoubleClickListener() {
                override fun onDoubleClick(view: View?) {
                    adapter.sendUpvoteToken(itemView, videoID)
                }
            }
        )
    }
}
