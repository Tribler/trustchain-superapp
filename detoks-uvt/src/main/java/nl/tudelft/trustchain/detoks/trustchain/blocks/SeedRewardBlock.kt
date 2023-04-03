package nl.tudelft.trustchain.detoks.trustchain.blocks

import android.util.Log
import nl.tudelft.ipv8.Peer
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.detoks.community.UpvoteCommunity
import nl.tudelft.trustchain.detoks.util.CommunityConstants

class SeedRewardBlock {
    companion object {
        /**
         * This function is to be used when this peer has to reward a peer for seeding this
         * peers video, which got upvoted by another peer.
         */
        fun createRewardBlock(
            videoId: String,
            seedingPeerKey: String,
            upvotingPeer: Peer
        ): TrustChainBlock? {
            val upvoteCommunity = IPv8Android.getInstance().getOverlay<UpvoteCommunity>()
            val myPeer = IPv8Android.getInstance().myPeer

            val peer = upvoteCommunity?.getPeers()
                ?.first { peer -> peer.publicKey.keyToBin().toHex() == (seedingPeerKey) }

            val transaction = mapOf(
                "videoID" to videoId,
                "videoCreatedBy" to myPeer.publicKey.keyToBin().toHex(),
                "videoSeededBy" to upvotingPeer.publicKey.keyToBin().toHex(),
                "videoUpvotedBy" to seedingPeerKey,
            )
            if (peer != null) {
                return upvoteCommunity.createProposalBlock(
                    CommunityConstants.GIVE_SEEDER_REWARD,
                    transaction,
                    peer.publicKey.keyToBin()
                )
            }

            return null
        }
    }
}
