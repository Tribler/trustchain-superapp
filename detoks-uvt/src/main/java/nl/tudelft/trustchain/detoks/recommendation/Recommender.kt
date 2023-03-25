package nl.tudelft.trustchain.detoks.recommendation

import android.util.Log
import nl.tudelft.ipv8.android.IPv8Android
import nl.tudelft.ipv8.attestation.trustchain.TrustChainBlock
import nl.tudelft.ipv8.keyvault.PublicKey
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
            recommendations.addAll(allTorrents.map{ it.asMediaInfo().videoID }.toMutableList())
            isInitialized = true
        }

        /**
         * Add a new recommendations to the list of recommendations.
         */
        fun addRecommendation(videoID: String) {
            recommendations.add(videoID)
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

        private fun getMostLikedVideoIDs(): List<String> {
            val upvoteCommunity = IPv8Android.getInstance().getOverlay<UpvoteCommunity>()
            val allUpvoteTokens = upvoteCommunity?.database?.getBlocksWithType(
                UpvoteTrustchainConstants.GIVE_UPVOTE_TOKEN) ?: return listOf()
            Log.i("DeToks", "RECEIVED ${allUpvoteTokens.size} UPVOTE TOKENS!")

            var videoHashMap: HashMap<String, Int> = hashMapOf()
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

        private fun getRandomVideoIDs(): List<String> {
            //TODO: implement his
            val randomIDs = mutableListOf<String>()
            return randomIDs
        }
    }
}
