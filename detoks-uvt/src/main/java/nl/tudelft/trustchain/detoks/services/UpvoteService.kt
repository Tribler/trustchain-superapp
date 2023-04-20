package nl.tudelft.trustchain.detoks.services

import nl.tudelft.trustchain.detoks.db.OwnedTokenManager
import nl.tudelft.trustchain.detoks.db.SentTokenManager
import nl.tudelft.trustchain.detoks.token.UpvoteToken
import nl.tudelft.trustchain.detoks.token.UpvoteTokenValidator
import nl.tudelft.trustchain.detoks.util.CommunityConstants

/**
 * Service to handle all upvote related actions.
 * @param context the context of the application.
 */
class UpvoteService (
    private val ownedTokenManager: OwnedTokenManager,
    private val sentTokenManager: SentTokenManager) {

    init {
        ownedTokenManager.createOwnedUpvoteTokensTable()
        sentTokenManager.createSentUpvoteTokensTable()
    }

    /**
     * Retrieves the last five videos that this peer upvoted.
     * @return the last five videos that this peer upvoted.
     */
    fun getFiveLatestUpvotedVideos() : List<String> {
        return sentTokenManager.getFiveLatestUpvotedVideos()
    }

    /**
     * Retrieves the last three videos that were upvoted of this peer,
     * that the peer created or seeded.
     * @return the last three videos that were upvoted of this peer, that the peer created or seeded.
     */
    fun getLatestThreeUpvotedVideos(): List<String> {
        return ownedTokenManager.getLatestThreeUpvotedVideos()
    }

    /**
     * Checks if the [UpvoteToken]s in the list are valid and returns a list of all valid tokens in the list.
     * @param upvoteTokens the list of [UpvoteToken]s to check.
     * @return a list of all valid [UpvoteToken]s that were in the [upvoteTokens]
     */
    fun getValidUpvoteTokens(upvoteTokens: List<UpvoteToken>): ArrayList<UpvoteToken> {

        val validTokens: ArrayList<UpvoteToken> = ArrayList()

        for (upvoteToken: UpvoteToken in upvoteTokens) {
            if (UpvoteTokenValidator.validateToken(upvoteToken)) {
                validTokens.add(upvoteToken)
            }
        }

        return validTokens
    }

    /**
     * Gets a list of reward tokens from the list of [UpvoteToken]s. The reward tokens are removed
     * from the initial list.
     * @param upvoteTokens the list of [UpvoteToken]s from which the reward tokens should be taken.
     * @return the list of [UpvoteToken]s that can be used as a reward.
     */
    fun getRewardTokens(upvoteTokens: ArrayList<UpvoteToken>) : ArrayList<UpvoteToken> {

        val rewardTokens: ArrayList<UpvoteToken> = ArrayList()

        for (i in 0 until CommunityConstants.SEED_REWARD_TOKENS) {
            if (upvoteTokens.isEmpty())
                break
            rewardTokens.add(upvoteTokens.removeFirst())
        }

        return rewardTokens
    }

    /**
     * Stores all [UpvoteToken]s in the database.
     * @param upvoteTokens the tokens to persist.
     */
    fun persistTokens(upvoteTokens: List<UpvoteToken>) {
        for (upvoteToken: UpvoteToken in upvoteTokens)
            ownedTokenManager.addReceivedToken(upvoteToken)
    }
}
