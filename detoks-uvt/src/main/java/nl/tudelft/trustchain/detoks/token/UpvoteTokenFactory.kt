package nl.tudelft.trustchain.detoks.token

import nl.tudelft.trustchain.detoks.db.SentTokenManager
import nl.tudelft.trustchain.detoks.exception.InvalidMintException
import nl.tudelft.trustchain.detoks.helpers.DateFormatter
import nl.tudelft.trustchain.detoks.util.CommunityConstants

/**
 * Factory class to mint [UpvoteToken]s.
 * @param sentTokenManager the sentTokenManager to use.
 */
class UpvoteTokenFactory(private val sentTokenManager: SentTokenManager) {


    init {
        sentTokenManager.createSentUpvoteTokensTable()
    }

    /**
     * Mints multiple [UpvoteToken]s based on the given input parameters.
     * @param videoID the id of the video.
     * @param myPubKey the public key of the minter.
     * @param publicKeySeeder the public key of the peer that seeded the most bytes
     * @param amount the amount of [UpvoteToken]s that should be minted
     * @throws InvalidMintException when the mint limit is exceeded or the tokens cannot be stored in the database.
     * @return the list of minted [UpvoteToken]s or the an empty list if an exception was thrown
     */
    fun tryMintMultipleTokens(videoID: String, myPubKey: String, publicKeySeeder: String, amount: Int): List<UpvoteToken> {
        val upvoteTokenList: ArrayList<UpvoteToken> = ArrayList()

        // Mint the required amount of tokens
        for (i in 0 until amount) {
            val nextToken = tryMintToken(videoID, myPubKey, publicKeySeeder)
            upvoteTokenList.add(nextToken)
        }

        return upvoteTokenList
    }

    /**
     * Mints an [UpvoteToken] based on the given input parameters.
     * @param videoID the id of the video.
     * @param publicKey the public key of the minter.
     * @param publicKeySeeder the public key of the peer that seeded the most bytes
     * @throws InvalidMintException when the mint limit is exceeded or the tokens cannot be stored in the database.
     * @return the minted [UpvoteToken]
     */
    fun tryMintToken(videoID: String, publicKey: String, publicKeySeeder: String): UpvoteToken {
        val lastUpvoteToken = sentTokenManager.getLastToken()
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

        val dbSuccess = sentTokenManager.addSentToken(newToken)

        if (!dbSuccess)
            throw InvalidMintException("Could not add the minted token to the database")

        return newToken
    }
}
