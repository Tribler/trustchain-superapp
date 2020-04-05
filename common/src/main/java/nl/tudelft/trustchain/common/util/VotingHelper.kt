package nl.tudelft.trustchain.common.util

import android.annotation.SuppressLint
import android.util.Log
import nl.tudelft.ipv8.attestation.trustchain.EMPTY_PK
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

/**
 * Helper class for creating votes proposals, casting and counting.
 * @param trustChainCommunity the community where the votes will be dealt.
 */
class VotingHelper(
    trustChainCommunity: TrustChainCommunity
) {
    private val votingBlock = "voting_block"

    private val trustChainHelper: TrustChainHelper = TrustChainHelper(trustChainCommunity)

    /**
     * Initiate a vote amongst a set of peers.
     * @param voteSubject the matter to be voted upon.
     * @param peers list of the public keys of those eligible to vote.
     */
    fun startVote(voteSubject: String, peers: List<PublicKey>) {
        // TODO: Add vote ID to increase probability of uniqueness.

        val voteList = JSONArray(peers)

        // Create a JSON object containing the vote subject, as well as a log of the eligible voters
        val voteJSON = JSONObject()
            .put("VOTE_SUBJECT", voteSubject)
            .put("VOTE_LIST", voteList)

        val transaction = voteJSON.toString()

        // Create any-counterparty block for the transaction
        trustChainHelper.createProposalBlock(transaction, EMPTY_PK, votingBlock)
    }

    /**
     * Respond to a proposal block on the trustchain, either agree with "YES" or disagree "NO".
     * @param vote boolean value indicating the decision.
     * @param proposalBlock TrustChainBlock of the proposalblock.
     */
    fun respondToVote(vote: Boolean, proposalBlock: TrustChainBlock) {

        // Parse the subject of the vote
        val proposalSubject = try {
            JSONObject(proposalBlock.transaction["message"].toString()).get("VOTE_SUBJECT")
        } catch (e: JSONException) {
            Log.e("vote-debug", "Invalidly formatted proposal block, perhaps not a proposal")
        }

        // Reply to the vote with YES or NO.
        val voteReply = if (vote) "YES" else "NO"

        // Create a JSON object containing the vote subject and the reply.
        val voteJSON = JSONObject()
            .put("VOTE_SUBJECT", proposalSubject)
            .put("VOTE_REPLY", voteReply)

        // Put the JSON string in the transaction's 'message' field.
        val transaction = mapOf("message" to voteJSON.toString())

        trustChainHelper.createAgreementBlock(proposalBlock, transaction)
    }

    /**
     * Return the tally on a vote proposal in a pair(yes, no).
     * @param voters list of the public keys of the eligible voters.
     * @param voteSubject the matter to be voted upon.
     * @param proposerKey the identifier of the vote proposer .
     * @return Pair<Int, Int> indicating the election results.
     */
    fun countVotes(
        voters: List<PublicKey>,
        voteSubject: String,
        proposerKey: ByteArray
    ): Pair<Int, Int> {

        // ArrayList for keeping track of already counted votes
        val votes: MutableList<String> = ArrayList()

        var yesCount = 0
        var noCount = 0

        // Crawl the chain of the proposer.
        for (it in trustChainHelper.getChainByUser(proposerKey)) {
            val blockPublicKey: PublicKey = defaultCryptoProvider.keyFromPublicBin(it.publicKey)

            // Check whether vote has already been counted
            if (votes.contains(it.publicKey.contentToString())) {
                continue
            }

            // Skip all blocks which are not voting blocks
            // and don't have a 'message' field in their transaction.
            if (it.type != votingBlock || !it.transaction.containsKey("message")) {
                continue
            }

            // Parse the 'message' field as JSON.
            val voteJSON = try {
                JSONObject(it.transaction["message"].toString())
            } catch (e: JSONException) {
                // Assume a malicious vote if it claims to be a vote but does not contain
                // proper JSON.
                handleInvalidVote(
                    "Block was a voting block but did not contain " +
                        "proper JSON in its message field: ${it.transaction["message"].toString()}."
                )
                continue
            }

            // Assume a malicious vote if it does not have a VOTE_SUBJECT.
            if (!voteJSON.has("VOTE_SUBJECT")) {
                handleInvalidVote("Block type was a voting block but did not have a VOTE_SUBJECT.")
                continue
            }

            // A block with another VOTE_SUBJECT belongs to another vote.
            if (voteJSON.get("VOTE_SUBJECT") != voteSubject) {
                // Block belongs to another vote.
                continue
            }

            // A block with the same subject but no reply is the original vote proposal.
            if (!voteJSON.has("VOTE_REPLY")) {
                // Block is the initial vote proposal because it does not have a VOTE_REPLY field.
                continue
            }

            // Check whether the voter is in voting list
            @SuppressLint
            if (!voters.any { v ->
                    val voteString = v.keyToBin()
                    voteString.contentEquals(blockPublicKey.keyToBin())
                }) {
                continue
            }

            // Add the votes, or assume a malicious vote if it is not YES or NO.
            when (voteJSON.get("VOTE_REPLY")) {
                "YES" -> {
                    yesCount++
                    votes.add(it.publicKey.contentToString())
                }
                "NO" -> {
                    noCount++
                    votes.add(it.publicKey.contentToString())
                }
                else -> handleInvalidVote("Vote was not 'YES' or 'NO' but: '${voteJSON.get("VOTE_REPLY")}'.")
            }
        }

        return Pair(yesCount, noCount)
    }

    /**
     * Helper function for debugging purposes
     */
    private fun handleInvalidVote(errorType: String) {
        Log.e("vote_debug", errorType)
    }
}
