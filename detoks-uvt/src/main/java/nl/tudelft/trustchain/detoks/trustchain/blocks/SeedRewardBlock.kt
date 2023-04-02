package nl.tudelft.trustchain.detoks.trustchain.blocks

import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.detoks.community.UpvoteCommunity
import nl.tudelft.trustchain.detoks.util.CommunityConstants

class SeedRewardBlock {
    /**
     * This function is to be used when this peer has to reward a peer for seeding this
     * peers video, which got upvoted by another peer.
     */
    fun createRewardBlock(seedingPeerPublicKey: String, upvotingPeer: Peer): TrustChainBlock? {
        val upvoteCommunity = IPv8Android.getInstance().getOverlay<UpvoteCommunity>()
        val myPeer = IPv8Android.getInstance().myPeer

        val transaction = mapOf(
            "videoID" to "TODO: REPLACE THIS WITH ACTUAL VIDEO ID",
            "videoCreatedBy" to myPeer.publicKey.keyToBin().toHex(),
            "videoSeededBy" to seedingPeerPublicKey.toByteArray().toHex(),
            "videoUpvotedBy" to upvotingPeer.publicKey.keyToBin().toHex(),
        )
        return upvoteCommunity?.createProposalBlock(
            CommunityConstants.GIVE_UPVOTE_TOKEN,
            transaction,
            seedingPeerPublicKey.toByteArray()
        );
    }
}
