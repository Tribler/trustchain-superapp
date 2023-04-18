package nl.tudelft.trustchain.detoks.services

import nl.tudelft.trustchain.detoks.db.OwnedTokenManager
import nl.tudelft.trustchain.detoks.token.UpvoteToken
import nl.tudelft.trustchain.detoks.token.UpvoteTokenValidator
import nl.tudelft.trustchain.detoks.util.CommunityConstants

/**
 * A service to handle all SeederReward related methods.
 *
 * @param ownedTokenManager The owned token manager that should be used.
 */
class SeedRewardService(private val ownedTokenManager: OwnedTokenManager) {

    init {
        ownedTokenManager.createOwnedUpvoteTokensTable()
    }

    /**
     * Handles the seed reward of granted by another peer. The method checks if the
     * peer received at least the community standard amount of valid tokens and adds the valid tokens
     * to the received token database.
     * @param upvoteTokens the List of received upvote tokens
     * @return True, if and only if, the list contains at least the community standard amount of
     * valid tokens.
     */
    fun handleReward(upvoteTokens: List<UpvoteToken>) : Boolean {

        val validTokens: ArrayList<UpvoteToken> = ArrayList()

        for (upvoteToken: UpvoteToken in upvoteTokens) {
            if (UpvoteTokenValidator.validateToken(upvoteToken)) {
                ownedTokenManager.addReceivedToken(upvoteToken)
                validTokens.add(upvoteToken)
            }
        }

        return CommunityConstants.SEED_REWARD_TOKENS <= validTokens.size
    }


}
