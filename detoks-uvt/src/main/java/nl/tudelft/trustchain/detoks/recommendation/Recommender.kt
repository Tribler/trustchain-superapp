package nl.tudelft.trustchain.detoks.recommendation

import android.util.Log
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.trustchain.detoks.TorrentManager
import nl.tudelft.trustchain.detoks.community.UpvoteCommunity
import nl.tudelft.trustchain.detoks.community.UpvoteTrustchainConstants
import kotlin.random.Random

class Recommender {
    companion object {
        private const val mostLikedProb: Float = 0.9F
        private var recommendations = mutableListOf<String>()
        private var isInitialized: Boolean = false

        /**
         * Initialize the list of recommendations with all the torrents in the TorrentManager
         * at start up to prevent user having to wait for recommendations.
         */
        fun initialize(torrentManager: TorrentManager) {
            if (isInitialized)
                return
            Log.i("DeToks", "Initializing Recommender...")
            val allTorrents: List<TorrentManager.TorrentHandler> = torrentManager.getAllTorrents()
            recommendations.addAll(allTorrents.map { it.asMediaInfo().videoID }.toMutableList())
            isInitialized = true
        }

        /**
         * Add a new recommendations to the list of recommendations.
         */
        fun addRecommendation(videoID: String) {
            if (!recommendations.contains(videoID))
                recommendations.add(videoID)
        }

        /**
         * Add new recommendations to the list of recommendations.
         */
        fun addRecommendations(videos: List<String>) {
            for (recommendation: String in videos)
                addRecommendation(recommendation)
        }
        /**
         * Get the next recommendation when there are recommendations left or update the list of
         * recommendations when there are no more recommendations.
         */
        fun getNextRecommendation(): String? {
            if (recommendations.size > 0) {
                val recommendedVideoID = recommendations.first()
                recommendations.remove(recommendedVideoID)
                return recommendedVideoID
            } else {
                createNewRecommendations()
                if (recommendations.size == 0)
                    return null
                return getNextRecommendation()
            }
        }

        /**
         * Update the list of recommendations.
         */
        private fun createNewRecommendations() {
            Log.i("DeToks", "Creating new recommendations...")

            // Recommend the most liked videos
            if (Random.nextFloat() <= mostLikedProb) {
                Log.i("DeToks", "Recommending MOST LIKED videos...")
                recommendations.addAll(getMostLikedVideoIDs())
            }
            // Recommend random videos to allow users to see new stuff they might like
            else {
                Log.i("DeToks", "Recommending RANDOM videos...")
                recommendations.addAll(getRandomVideoIDs())
            }
            Log.i("DeToks", "Recommendations: $recommendations")
        }

        /**
         * Return a List in sorted order with the most liked videos in general at the start.
         * Check if a block is an agreement block to make sure it is an upvote and not the creation
         * block coupled to the uploading of a video.
         */
        private fun getMostLikedVideoIDs(): List<String> {
            val upvoteCommunity = IPv8Android.getInstance().getOverlay<UpvoteCommunity>()
            val allUpvoteTokens = upvoteCommunity?.database?.getBlocksWithType(
                UpvoteTrustchainConstants.GIVE_UPVOTE_TOKEN) ?: return listOf()
            Log.i("DeToks", "RECEIVED ${allUpvoteTokens.size} UPVOTE TOKENS!")

            val videoHashMap: HashMap<String, Int> = hashMapOf()
            for (block: TrustChainBlock in allUpvoteTokens) {
                if (block.isAgreement) {
                    val videoID: String = block.transaction["videoID"] as String
                    Log.i("DeToks", "Like for video: $videoID")

                    if (videoHashMap.containsKey(videoID)) {
                        val currentLikes = videoHashMap[videoID]
                        videoHashMap[videoID] = currentLikes!! + 1
                    } else {
                        videoHashMap[videoID] = 1
                    }
                }
            }
            videoHashMap.toSortedMap(Comparator.reverseOrder())
            return videoHashMap.keys.toList()
        }

        /**
         * Return a list of random video IDs by getting all video IDs. Check if the blocks are
         * proposal blocks to avoid having the same video ID being present multiple times.
         */
        private fun getRandomVideoIDs(): List<String> {
            val upvoteCommunity = IPv8Android.getInstance().getOverlay<UpvoteCommunity>()
            val allUpvoteTokens = upvoteCommunity?.database?.getBlocksWithType(
                UpvoteTrustchainConstants.GIVE_UPVOTE_TOKEN) ?: return listOf()
            var proposalTokens = allUpvoteTokens.filter { it.isProposal }
            proposalTokens = proposalTokens.shuffled()
            Log.i("DeToks", "Random video IDs: ${proposalTokens.size}")
            for (token in proposalTokens) {
                Log.i("DeToks", "\tVideo ID: ${token.transaction["videoID"]}")
            }
            return proposalTokens.map { it.transaction["videoID"].toString() }.toList()
        }

        /**
         * Start a crawl on the network to find new upvoted content. The Recommender will be
         * filled with received recommendations when the peers send a message back.
         */
        private fun requestUpvotedContent() {
            val upvoteCommunity = IPv8Android.getInstance().getOverlay<UpvoteCommunity>()
            upvoteCommunity?.requestRecommendations()
        }

        //TODO: remove these, for debugging only
        fun recommendMostLiked() {
            getMostLikedVideoIDs()
        }

        fun recommendRandom() {
            getRandomVideoIDs()
        }

        fun requestRecommendations() {
            requestUpvotedContent()
        }
    }
}
