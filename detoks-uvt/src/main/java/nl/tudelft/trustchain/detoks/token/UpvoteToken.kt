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
import nl.tudelft.trustchain.detoks.community.UpvoteTrustChainConstants
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
     * */
    fun sendUpvoteToken(itemView: View) {

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

        val currentVideoHash = upvoteCommunity.torrentManager?.getHashOfCurrentVideo()!!.hexToBytes()
        val proposalBlock = upvoteCommunity.database.getBlockWithHash(currentVideoHash)
        if (proposalBlock != null) {
            val linkedBlocks = upvoteCommunity.database.getAllLinked(proposalBlock)
            val alreadyLiked = linkedBlocks.find { it.type == UpvoteTrustChainConstants.GIVE_UPVOTE_TOKEN
                && it.publicKey.toHex().contentEquals(myPubKey) && it.isAgreement}
            if (alreadyLiked != null) {
                Log.i("DeToks", "You already liked this video, cannot like again")
                Toast.makeText(
                    itemView.context,
                    "Cannot like a video more than once!",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            upvoteCommunity.createAgreementBlock(proposalBlock, proposalBlock.transaction)
            val seedingMagnetUri = upvoteCommunity.torrentManager?.seedLikedVideo()
            upvoteCommunity.torrentManager?.mvpSeeder()
            var toastMsg: String? = if (seedingMagnetUri != null) {
                "Upvoted, now seeding this video: $seedingMagnetUri"
            } else {
                "Upvoted, but failed to seed this video"
            }
            Log.i("DeToks", "Agreement block created!")
            Toast.makeText(
                itemView.context,
                toastMsg  ,
                Toast.LENGTH_LONG
            ).show()

            var toastMessage: String?

            if (chosenSeederPublicKey == null) {
                // Could not find the seeder, all rewards go to the peer who posted the video
                Log.i("Detoks", "Could not find the seeder of this video you liked, all minted tokens will go the peer who posted this video you upvoted")
                chosenSeederPublicKey = proposalBlock.publicKey.toHex()
            }

            Toast.makeText(
                itemView.context,
                "reward seeder with this pub key: \n $chosenSeederPublicKey",
                Toast.LENGTH_LONG
            ).show()

            try {
                val upvoteTokenList: ArrayList<UpvoteToken> = ArrayList()
                val tokenIDList: ArrayList<Int> = ArrayList()

                var dbSuccess: Boolean

                // Mint the required amount of tokens to upvote the video
                while (upvoteTokenList.size < CommunityConstants.TOKENS_SENT_PER_UPVOTE) {
                    val nextToken = tryMintToken(itemView.context, proposalBlock.blockId, myPubKey, chosenSeederPublicKey)
                    dbSuccess = SentTokenManager(itemView.context).addSentToken(nextToken)

                    if (!dbSuccess)
                        throw InvalidMintException("Could not add the minted token to the database")
                    upvoteTokenList.add(nextToken)
                    tokenIDList.add(nextToken.tokenID)
                }
                val sendSuccess = upvoteCommunity.sendUpvoteToken(upvoteTokenList, proposalBlock.publicKey)
                toastMessage = if (sendSuccess) {
                    "Successfully sent the token ${tokenIDList.joinToString(", ")} \n to the creator of ${proposalBlock.blockId}"
                } else {
                    "Failed to sent the token ${tokenIDList.joinToString(", ")} \n to the creator of ${proposalBlock.blockId}"
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
                Toast.LENGTH_LONG
            ).show()

        } else {
            Toast.makeText(
                itemView.context,
                "Attempted to find a proposal Block with hash: \n ${currentVideoHash.toHex()}, \nThis video does not have a proposal block attached to it \n and is thus not posted by anyone",
                Toast.LENGTH_LONG
            ).show()
            return
        }
    }

    /**
     * Sets a listener to like a video by double tapping the screen
     */
    fun setLikeListener(itemView: View) {
        val adapter = this

        itemView.setOnClickListener(
            object : DoubleClickListener() {
                override fun onDoubleClick(view: View?) {
                    adapter.sendUpvoteToken(itemView)
                }
            }
        )
    }
}
