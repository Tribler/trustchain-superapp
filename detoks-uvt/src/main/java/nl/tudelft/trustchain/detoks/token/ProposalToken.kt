package nl.tudelft.trustchain.detoks.token

import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.ANY_COUNTERPARTY_PK
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.detoks.TorrentManager
import nl.tudelft.trustchain.detoks.community.UpvoteCommunity
import nl.tudelft.trustchain.detoks.community.UpvoteTrustchainConstants

class ProposalToken {

    /**
     * This function is to be used when a peer posts video with video ID X:
     * the idea is that a proposal block is created everytime a peer posts a video
     * This proposal block is signed by the peer/user that uploaded the video.
     * This proposal block can be paired with an agreement block created and signed by
     * anyone who gives the video X a like/upvote.
     * (except the peer that initiated the proposal block)
     */
    private fun createProposalToken(): TrustChainBlock? {
        val upvoteCommunity = IPv8Android.getInstance().getOverlay<UpvoteCommunity>()
        val myPeer = IPv8Android.getInstance().myPeer

        val transaction = mapOf(
            "videoID" to "TODO: REPLACE THIS WITH ACTUAL VIDEO ID",
            "heartTokenGivenBy" to ANY_COUNTERPARTY_PK.toHex(),
            "heartTokenGivenTo" to myPeer.publicKey.keyToBin().toHex()
        )
        val proposalBlock = upvoteCommunity?.createProposalBlock(
            UpvoteTrustchainConstants.GIVE_UPVOTE_TOKEN,
            transaction,
            ANY_COUNTERPARTY_PK
        )
        return proposalBlock
    }

    /**
     * Sets a listener to create a proposal token after a long press has been detected.
     * We do not have a post video functionality yet
     * A long press of 2 seconds represents/simulates a peer having posted a video
     */
    fun setPostVideoListener(proposalSendButton: Button, itemView: View, torrentManager: TorrentManager) {
        proposalSendButton.setOnClickListener{
            val proposalBlock = createProposalToken()
            val upvoteCommunity = IPv8Android.getInstance().getOverlay<UpvoteCommunity>()
            val hash = proposalBlock?.calculateHash()!!
            val myPeer = IPv8Android.getInstance().myPeer
            val message = "Button Clicked! Your public key: " +
                "${myPeer.publicKey.keyToBin().toHex()} and member id:\n" +
                "$myPeer.mid has created a proposalblock on this timestamp: ${proposalBlock.timestamp} \n" +
                "The hash of this block is ${hash.toHex()}, corresponding hashCode is: ${hash.hashCode()} \n" +
                "the block Id of this proposal block is: ${proposalBlock.blockId} \n" +
                "the linked block id is: ${proposalBlock.linkedBlockId}\n"
            // torrentManager.addNewVideo(hash.toHex(), proposalBlock.timestamp.toString(), proposalBlock.blockId)
            val torrentInfo = torrentManager.getSeedableTorrents().get(0)
            val magnetURI = torrentManager.seedTorrent(torrentInfo)

            if (magnetURI == null) {
                Log.i("DeToks", "Seeding failed!")
            } else {
                Log.i("DeToks", "Seeding succeeded!")
            }

            if (magnetURI != null) {
                upvoteCommunity?.sendVideoData(magnetURI, hash.toHex())
            }

            Log.i("DeToks", message)
            Toast.makeText(
                itemView.context,
                message,
                Toast.LENGTH_SHORT
            ).show()
            createProposalToken()
        }
    }
}
