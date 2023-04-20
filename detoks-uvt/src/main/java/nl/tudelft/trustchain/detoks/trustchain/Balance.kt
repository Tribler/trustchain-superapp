package nl.tudelft.trustchain.detoks.trustchain

import android.text.format.DateUtils
import android.util.Log
import android.widget.TextView
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.util.toHex
import nl.tudelft.trustchain.detoks.community.UpvoteCommunity
import nl.tudelft.trustchain.detoks.util.CommunityConstants

class Balance {

    fun dailyBalanceCheckpoint(tokensSent: TextView, tokensReceived: TextView, tokensBalance: TextView) {
        val upvoteCommunity = IPv8Android.getInstance().getOverlay<UpvoteCommunity>()!!
        val myPublicKey = IPv8Android.getInstance().myPeer.publicKey.keyToBin()
        val latestBalanceCheckpoint = upvoteCommunity.database.getLatest(myPublicKey, CommunityConstants.BALANCE_CHECKPOINT)
        if (latestBalanceCheckpoint != null) {
            // If date is today then don't do anything because there is already a block for today
            if (DateUtils.isToday(latestBalanceCheckpoint.timestamp.time)) return
            val blocksToProcess = upvoteCommunity.database.crawl(myPublicKey, latestBalanceCheckpoint.sequenceNumber.toLong(), upvoteCommunity.database.getBlockCount(myPublicKey))
            var sent = latestBalanceCheckpoint.transaction.get("sent")!!.toString().toInt();
            var received = latestBalanceCheckpoint.transaction.get("received")!!.toString().toInt();
            for (block: TrustChainBlock in blocksToProcess){
                if (block.type.equals(CommunityConstants.GIVE_UPVOTE_TOKEN)) {
                    if (block.isAgreement && block.linkPublicKey.toHex().contentEquals(myPublicKey.toHex())) {
                        received++
                    }
                    if (block.isAgreement && block.publicKey.toHex().contentEquals(myPublicKey.toHex())) {
                        sent++
                    }
                }
            }
            val transaction = mapOf(
                "sent" to sent,
                "received" to received,
                "balance" to received - sent
            )
            upvoteCommunity.createProposalBlock(CommunityConstants.BALANCE_CHECKPOINT, transaction, myPublicKey)
        } else {
            // else case : first checkpoint block
            // get all balances
            val (sent, received, balance) = checkTokenBalance(tokensSent, tokensReceived, tokensBalance)
            val transaction = mapOf(
                "sent" to sent,
                "received" to received,
                "balance" to balance
            )
            upvoteCommunity.createProposalBlock(CommunityConstants.BALANCE_CHECKPOINT, transaction, myPublicKey)
        }
    }

    fun checkTokenBalance(tokensSent: TextView, tokensReceived: TextView, tokensBalance: TextView):Triple<Int, Int, Int> {
        val upvoteCommunity = IPv8Android.getInstance().getOverlay<UpvoteCommunity>()!!
        val allBlocks = upvoteCommunity.database.getBlocksWithType(CommunityConstants.GIVE_UPVOTE_TOKEN)
        var sent  = 0;
        var received = 0;
        for (block: TrustChainBlock in allBlocks) {
            if (block.isAgreement && block.linkPublicKey.toHex().contentEquals(IPv8Android.getInstance().myPeer.publicKey.keyToBin().toHex())) {
                received++
            }
            if (block.isAgreement && block.publicKey.toHex().contentEquals(IPv8Android.getInstance().myPeer.publicKey.keyToBin().toHex())) {
                sent++
            }
        }
        Log.i("DeToks", "Tokens sent: $sent received: $received")
        tokensSent.text = "$sent"
        tokensReceived.text = "$received"
        tokensBalance.text = "${received - sent}"
        return Triple(sent, received, received-sent)
    }
}
