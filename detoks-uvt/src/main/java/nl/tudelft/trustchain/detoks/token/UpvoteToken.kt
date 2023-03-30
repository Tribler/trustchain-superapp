package nl.tudelft.trustchain.detoks.token

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.trustchain.detoks.community.UpvoteCommunity
import nl.tudelft.trustchain.detoks.db.SentTokenManager
import nl.tudelft.trustchain.detoks.helpers.DateFormatter
import nl.tudelft.trustchain.detoks.exception.InvalidMintException
import nl.tudelft.trustchain.detoks.exception.PeerNotFoundException
import nl.tudelft.trustchain.detoks.helpers.DoubleClickListener
import java.util.*

class UpvoteToken constructor(
    val tokenID: Int,
    val date: String,
    val publicKeyMinter: String,
    val videoID: String
) {

    companion object {
        fun tryMintToken(context: Context, videoID: String, publicKey: String): UpvoteToken {
            SentTokenManager(context).createSentUpvoteTokensTable()
            val lastUpvoteToken = SentTokenManager(context).getLastToken()
            // Check if we have sent a token already today
            val today = DateFormatter.startOfToday()
            val newToken: UpvoteToken

            // Check if a new sequence should be started
            if (lastUpvoteToken == null || DateFormatter.stringToDate(lastUpvoteToken.date).before(today)) {
                newToken = UpvoteToken(0, DateFormatter.todayAsString(), publicKey, videoID)
            } else if (lastUpvoteToken.tokenID > -1 && lastUpvoteToken.tokenID < 9) {
                val nextId = lastUpvoteToken.tokenID + 1
                newToken = UpvoteToken(nextId, DateFormatter.todayAsString(), publicKey, videoID)
            } else {
                throw InvalidMintException("Mint limit exceeded")
            }

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
    fun sendUpvoteToken(itemView: View, videoID: TextView, proposalBlockHash: TextView) {
        val upvoteCommunity = IPv8Android.getInstance().getOverlay<UpvoteCommunity>()
        val myPubKey = upvoteCommunity?.myPeer?.publicKey.toString()
        //val upvoteToken = UpvoteToken(1, "1679006615", "12345678910", 1)
        //val toastMessage = upvoteCommunity?.sendUpvoteToken(upvoteToken.tokenID.toString(), localToGMT(upvoteToken.date.toLong()).toString(), upvoteToken.publicKeyMinter, upvoteToken.videoID.toString())
        var toastMessage: String?

        try {
            val nextToken = tryMintToken(itemView.context, videoID.text.toString(), myPubKey)
            val dbSuccess = SentTokenManager(itemView.context).addSentToken(nextToken)
            val sendSuccess = upvoteCommunity?.sendUpvoteToken(nextToken)

            toastMessage = if (dbSuccess && sendSuccess == true) {
                "Successfully sent the token ${nextToken.tokenID} to the creator of ${nextToken.videoID}"
            } else {
                "Failed to sent the token ${nextToken.tokenID} to the creator of ${nextToken.videoID}"
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
        // getBlockHash Method below might fail to get the proposal block if it is not in this peer's truststore
        val proposalBlock = upvoteCommunity?.database?.getBlockWithHash(proposalBlockHash.text.toString().hexToBytes())
        if (proposalBlock != null) {
            upvoteCommunity.createAgreementBlock(proposalBlock, proposalBlock.transaction)
            Log.i("DeToks", "Agreement block created!")
        } else {
            Toast.makeText(
                itemView.context,
                "This video does not have a proposal block attached to it and is thus not posted by anyone",
                Toast.LENGTH_SHORT
            ).show()
        }
        //TODO when a video is posted by peer A on phone Ap by long pressing,
        // how does peer B get the video posted by peer A on its phone Bp?
        // peer B first: needs to have peer A's posted video displayed on phone Bp
        // only when the above condition is satisfied can peer B create an agreement block
        // for peer A's proposal block that was created when peer posted a video (thus liking the video posted by peer A)

        // TODO: Currently, it seems that a peer can only like a video created by itself
        //       when we can distribute a video added by a peer to another peer B, peer B will be able to like that video
    }

    /**
     * Sets a listener to like a video by double tapping the screen
     */
    fun setLikeListener(itemView: View, videoID: TextView, proposalBlockHash: TextView) {
        val adapter = this

        itemView.setOnClickListener(
            object : DoubleClickListener() {
                override fun onDoubleClick(view: View?) {
                    adapter.sendUpvoteToken(itemView, videoID, proposalBlockHash)
                }
            }
        )
    }
}