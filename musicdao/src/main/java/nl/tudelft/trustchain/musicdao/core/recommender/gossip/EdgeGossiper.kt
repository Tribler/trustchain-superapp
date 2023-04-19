package nl.tudelft.trustchain.musicdao.core.recommender.gossip

import com.frostwire.jlibtorrent.*
import kotlinx.coroutines.*
import mu.KotlinLogging
import nl.tudelft.trustchain.musicdao.core.recommender.graph.TrustNetwork

/**
 * Gossips edges in the network to keep graphs synced
 */

lateinit var edgeGossiperInstance: EdgeGossiper
const val GOSSIP_DELAY: Long = 10000
const val DOWNLOAD_DELAY: Long = 20000

const val TORRENT_ATTEMPTS_THRESHOLD = 1
const val EVA_RETRIES = 10
private val logger = KotlinLogging.logger {}

class EdgeGossiper(
    private val sessionManager: SessionManager,
    private val recommenderCommunityBase: RecommenderCommunityBase,
    private val toastingEnabled: Boolean,
    private val trustNetwork: TrustNetwork
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        fun getInstance(
            sessionManager: SessionManager,
            recommenderCommunity: RecommenderCommunityBase,
            toastingEnabled: Boolean = true,
            trustNetwork: TrustNetwork
        ): EdgeGossiper {
            if (!::edgeGossiperInstance.isInitialized) {
                edgeGossiperInstance = EdgeGossiper(sessionManager, recommenderCommunity, toastingEnabled, trustNetwork)
            }
            return edgeGossiperInstance
        }
    }

    fun start() {
        scope.launch {
            gossipNodeToNodeEdges()
        }
        scope.launch {
            gossipNodeToSongEdges()
        }
    }

    private suspend fun gossipNodeToSongEdges() {
        TODO("Not yet implemented")
    }

    private suspend fun gossipNodeToNodeEdges() {
        TODO("Not yet implemented")
    }
}
