package nl.tudelft.trustchain.common.util

import android.util.Log
import nl.tudelft.ipv8.attestation.trustchain.EMPTY_PK
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.attestation.trustchain.TrustChainCommunity
import nl.tudelft.ipv8.keyvault.PublicKey
import nl.tudelft.ipv8.keyvault.defaultCryptoProvider
import nl.tudelft.ipv8.util.hexToBytes
import nl.tudelft.ipv8.util.toHex
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Helper class for creating votes proposals, casting and counting.
 * @param trustChainCommunity the community where the votes will be dealt.
 */
class VotingHelper(
    trustChainCommunity: TrustChainCommunity
) {
    private val votingBlock = "voting_block"
    val myPublicKey = trustChainCommunity.myPeer.publicKey
    private val trustChainHelper: TrustChainHelper = TrustChainHelper(trustChainCommunity)

    /**
     * Initiate a vote amongst a set of peers.
     * @param voteSubject the matter to be voted upon.
     * @param peers list of the public keys of those eligible to vote.
     */
    fun createProposal(voteSubject: String, peers: List<PublicKey>, mode: VotingMode) {
        val voteList = JSONArray(peers.map { i -> i.keyToBin().toHex() })

        // Create a JSON object containing the vote subject, as well as a log of the eligible voters
        val voteJSON = JSONObject()
            .put("VOTE_PROPOSER", myPublicKey.keyToBin().toHex())
            .put("VOTE_SUBJECT", voteSubject)
            .put("VOTE_MODE", mode)
            .put("VOTE_LIST", voteList)

        val transaction = voteJSON.toString()

        // Create any-counterparty block for the transaction
        trustChainHelper.createProposalBlock(transaction, EMPTY_PK, votingBlock)
    }

    /**
     * Respond to a proposal block on the trustchain, either agree with "YES" or disagree with "NO".
     * @param vote boolean value indicating the decision.
     * @param proposalBlock TrustChainBlock of the proposalblock.
     */
    fun respondToProposal(vote: Boolean, proposalBlock: TrustChainBlock) {
        val proposalSubject = getVotingBlockAttributesByKey(proposalBlock, "VOTE_SUBJECT")

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

            // Skip all blocks which are not voting blocks.
            if (it.type != votingBlock) {
                continue
            }

            // A block with another VOTE_SUBJECT belongs to another vote.
            if (getVotingBlockAttributesByKey(it, "VOTE_SUBJECT") != voteSubject) {
                // Block belongs to another vote.
                continue
            }

            // If no vote_reply present, we are dealing with the proposal block
            if (!hasVotingBlockAttributeByKey(it, "VOTE_REPLY")) continue

            // Check whether the voter is in voting list.
            if (!voters.any { v ->
                val voteString = v.keyToBin()
                voteString.contentEquals(blockPublicKey.keyToBin())
            }
            ) {
                continue
            }

            // Add the votes, or assume a malicious vote if it is not YES or NO.
            when (val voteReply = getVotingBlockAttributesByKey(it, "VOTE_REPLY")) {
                "YES" -> {
                    yesCount++
                    votes.add(it.publicKey.contentToString())
                }
                "NO" -> {
                    noCount++
                    votes.add(it.publicKey.contentToString())
                }
                else -> handleInvalidVote(
                    it,
                    "Vote was not 'YES' or 'NO' but: '$voteReply}'."
                )
            }
        }

        return Pair(yesCount, noCount)
    }

    /**
     * Check if the user has casted a vote upon a proposal already.
     */
    fun castedByPeer(block: TrustChainBlock, publicKey: PublicKey): Pair<Int, Int> {
        val subject = getVotingBlockAttributesByKey(block, "VOTE_SUBJECT")
        return countVotes(listOf(publicKey), subject, block.publicKey)
    }

    /**
     * Return a list of voters' public keys parsed from the block.
     */
    fun getVoters(block: TrustChainBlock): List<PublicKey> {

        val jsonKeys = try {
            JSONArray(getVotingBlockAttributesByKey(block, "VOTE_LIST"))
        } catch (e: JSONException) {
            handleInvalidVote(block, "Voting block did not contain proper JSON for a voter list.")
        }

        val publicKeys: MutableList<PublicKey> = ArrayList()
        for (i in 0 until jsonKeys.length()) {
            val string = try {
                jsonKeys.get(i)
            } catch (e: JSONException) {
                handleInvalidVote(block, "Voting block did not have a value at index $i.")
            }

            val hexKey = string.toString().hexToBytes()
            if (!defaultCryptoProvider.isValidPublicBin(hexKey)) {
                handleInvalidVote(
                    block,
                    "A public key in the voter list was not valid. Its value was: $string."
                )
            }

            val key = defaultCryptoProvider.keyFromPublicBin(hexKey)
            publicKeys.add(key)
        }

        return publicKeys
    }

    /**
     * Return true when the voting is complete, meaning a threshold vote has reached its
     * threshold, or a yes/no vote has received votes from all eligible voters.
     */
    fun votingIsComplete(block: TrustChainBlock, threshold: Int = -1): Boolean {
        return getVotingProgressStatus(block, threshold) >= 100 || getVotingProgressStatus(
            block,
            threshold
        ) == -1
    }

    /**
     * Return the percentage of required votes or -1 when a threshold is not made but all votes are received.
     */
    fun getVotingProgressStatus(block: TrustChainBlock, thresholdPercentage: Int = -1): Int {
        fun percentage(a: Int, b: Int): Int {
            if (b == 0) {
                return 0
            }

            return ((a.toDouble() / b) * 100).roundToInt()
        }

        val voters = getVoters(block)
        val threshold = ceil((thresholdPercentage / 100.0) * voters.size).toInt()
        val voteSubject = getVotingBlockAttributesByKey(block, "VOTE_SUBJECT")
        val proposerKey = getVotingBlockAttributesByKey(block, "VOTE_PROPOSER")
        val voteMode = getVotingBlockAttributesByKey(block, "VOTE_MODE")

        val hexKey = proposerKey.hexToBytes()
        if (!defaultCryptoProvider.isValidPublicBin(hexKey)) {
            handleInvalidVote(
                block,
                "The proposer key from the block was not valid. Its value was: $proposerKey."
            )
        }

        val key = defaultCryptoProvider.keyFromPublicBin(hexKey)

        val count = countVotes(voters, voteSubject, key.keyToBin())
        val yescount = count.first
        val nocount = count.second

        val mode = try {
            VotingMode.valueOf(voteMode)
        } catch (e: IllegalArgumentException) {
            handleInvalidVote(block, "The VotingMode was invalid, its value was $voteMode.")
        }

        return if (mode == VotingMode.THRESHOLD) {
            if (thresholdPercentage == -1) {
                throw IllegalArgumentException("VotingMode was set to THRESHOLD but threshold was not specified as function argument.")
            } else if (yescount + nocount == voters.size && yescount < threshold) {
                return -1
            }
            percentage(yescount, threshold)
        } else {
            percentage(yescount + nocount, voters.size)
        }
    }

    /**
     * Checks if a block possesses a specific attribute, without retrieving it explicitly
     */
    private fun hasVotingBlockAttributeByKey(block: TrustChainBlock, key: String): Boolean {

        // Skip all blocks which are not voting blocks
        // and don't have a 'message' field in their transaction.
        if (block.type != votingBlock || !block.transaction.containsKey("message")) {
            handleInvalidVote(
                block,
                "Block was not a voting block or did not contain a 'message' field in its transaction."
            )
        }

        val voteJSON = try {
            JSONObject(block.transaction["message"].toString())
        } catch (e: JSONException) {
            handleInvalidVote(block, "Voting block did not contain proper JSON.")
        }

        return voteJSON.has(key)
    }

    /**
     * Return the string value for a JSON tag in a voting block.
     */
    private fun getVotingBlockAttributesByKey(block: TrustChainBlock, key: String): String {
        if (hasVotingBlockAttributeByKey(block, key)) {
            return JSONObject(block.transaction["message"].toString()).get(key).toString()
        }
        handleInvalidVote(block, "Voting JSON did not have a value for key '$key'.")
    }

    /**
     * Throw an exception when an invalid vote is encountered.
     */
    private fun handleInvalidVote(block: TrustChainBlock, errorType: String): Nothing {
        Log.e(
            "vote_debug",
            "Encountered an invalid voting block with ID ${block.blockId}." +
                "The reason for invalidity was: $errorType"
        )
        throw Exception(errorType)
    }
}

enum class VotingMode {
    YESNO, THRESHOLD
}
