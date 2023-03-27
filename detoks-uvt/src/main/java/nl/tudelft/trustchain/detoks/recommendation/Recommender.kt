package nl.tudelft.trustchain.detoks.recommendation

import android.util.Log
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.trustchain.detoks.TorrentManager
import nl.tudelft.trustchain.detoks.community.UpvoteCommunity
import nl.tudelft.trustchain.detoks.community.UpvoteTrustchainConstants
import kotlin.random.Random

enum class RecommendationType {
    PEERS, MOST_LIKED, RANDOM
}

class Recommender {
    companion object {
        private const val MAX_NUM_RECOMMENDATIONS = 20
        private const val peersWeight: Float = 0.7F
        private const val mostLikedWeight: Float = 0.25F
        private const val randomWeight: Float = 1F - mostLikedWeight - peersWeight
        private var isInitialized: Boolean = false
        private var recommendations = mutableListOf<String>()
        private var mostLikedRecommendations = mutableListOf<String>()
        private var peerRecommendations = mutableListOf<String>()
        private var randomRecommendations = mutableListOf<String>()

        /**
         * Initialize the list of recommendations with all the torrents in the TorrentManager
         * at start up to prevent user having to wait for recommendations.
         */
        fun initialize(torrentManager: TorrentManager) {
            if (isInitialized)
                return
            Log.i("DeToks", "Initializing Recommender...")
            Log.i("DeToks", "Recommendation Weights: \n\tPEERS: $peersWeight " +
                "\n\tMOST LIKED: $mostLikedWeight \n\tRANDOM: $randomWeight")
            val allTorrents: List<TorrentManager.TorrentHandler> = torrentManager.getAllTorrents()
            recommendations.addAll(allTorrents.map { it.asMediaInfo().videoID }.toMutableList())
            isInitialized = true
        }

        /**
         * Add a new recommendations to the list of recommendations.
         */
        private fun addRecommendation(videoID: String, recommendationType: RecommendationType) {
            when (recommendationType) {
                RecommendationType.PEERS -> {
                    if (!peerRecommendations.contains(videoID))
                        peerRecommendations.add(videoID)
                }
                RecommendationType.MOST_LIKED -> {
                    if (!mostLikedRecommendations.contains(videoID))
                        mostLikedRecommendations.add(videoID)
                }
                RecommendationType.RANDOM -> {
                    if (!randomRecommendations.contains(videoID))
                        randomRecommendations.add(videoID)
                }
            }

        }

        /**
         * Add new recommendations to the list of recommendations.
         */
        fun addRecommendations(videos: List<String>, recommendationType: RecommendationType) {
            for (recommendation: String in videos)
                addRecommendation(recommendation, recommendationType)
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
            Log.i("DeToks", "Creating PEERS recommendations...")
            val upvoteCommunity = IPv8Android.getInstance().getOverlay<UpvoteCommunity>()
            upvoteCommunity?.requestRecommendations()
            Log.i("DeToks", "Creating MOST LIKED recommendations...")
            addRecommendations(getMostLikedVideoIDs(), RecommendationType.MOST_LIKED)
            Log.i("DeToks", "Creating RANDOM recommendations...")
            addRecommendations(getRandomVideoIDs(), RecommendationType.RANDOM)

            for (i in 0 until MAX_NUM_RECOMMENDATIONS) {
                val prob = Random.nextFloat()
                var newVideoID: String? = null
                if (prob <= peersWeight) {
                    if (peerRecommendations.size > 0)
                        newVideoID = peerRecommendations.removeFirst()
                } else if (prob <= peersWeight + mostLikedWeight) {
                    if (mostLikedRecommendations.size > 0)
                        newVideoID = mostLikedRecommendations.removeFirst()
                } else {
                    if (randomRecommendations.size > 0)
                        newVideoID = randomRecommendations.removeFirst()
                }
                if (newVideoID != null) {
                    recommendations.add(newVideoID)
                    peerRecommendations.remove(newVideoID)
                    mostLikedRecommendations.remove(newVideoID)
                    randomRecommendations.remove(newVideoID)
                }
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

            val keys = videoHashMap.toSortedMap(compareByDescending { videoHashMap[it] }).keys

            return keys.toList()
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
