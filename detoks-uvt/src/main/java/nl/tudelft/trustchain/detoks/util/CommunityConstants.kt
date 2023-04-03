package nl.tudelft.trustchain.detoks.util

object CommunityConstants {

    //UpvoteCommunity constants
    const val GIVE_UPVOTE_TOKEN = "give_upvote_token_block"
    const val GIVE_SEEDER_REWARD = "give_seeder_reward"
    const val BALANCE_CHECKPOINT = "balance_checkpoint"

    // Minting
    const val TOKENS_SENT_PER_UPVOTE = 3
    private const val DAILY_VIDEO_UPVOTE_LIMIT = 100
    const val DAILY_MINT_LIMIT = TOKENS_SENT_PER_UPVOTE * DAILY_VIDEO_UPVOTE_LIMIT
    const val SEED_REWARD_TOKENS = 1

    //MessageID constants
    const val UPVOTE_TOKEN = 1
    const val MAGNET_URI_AND_HASH = 2
    const val UPVOTE_VIDEO = 3
    const val SEED_REWARD = 4
}
